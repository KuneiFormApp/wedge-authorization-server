package com.kuneiform.infraestructure.adapter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.kuneiform.infraestructure.config.properties.WedgeConfigProperties;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.stereotype.Component;

/**
 * Hybrid Redis + Caffeine implementation of OAuth2AuthorizationService.
 *
 * <p>Strategy:
 *
 * <ul>
 *   <li><b>L1 (Local):</b> Caffeine Cache for ultra-fast reads (Authorization & Indexes).
 *   <li><b>L2 (Remote):</b> Redis for persistence and distributed state.
 * </ul>
 *
 * <p>Active when {@code wedge.token-storage.type=redis}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "wedge.token-storage.type", havingValue = "redis")
public class RedisOAuth2AuthorizationServiceAdapter implements OAuth2AuthorizationService {

  private final WedgeConfigProperties config;
  private final RedisConnectionFactory redisConnectionFactory;

  // L1 Caches (In-Memory)
  private Cache<String, OAuth2Authorization> localAuthCache;
  private Cache<String, String> localIndexCache;

  // L2 Templates (Redis)
  private RedisTemplate<String, OAuth2Authorization> redisAuthTemplate;
  private RedisTemplate<String, String> redisIndexTemplate;

  private String namespace;
  private static final String AUTH_KEY_PREFIX = "auth:";
  private static final String INDEX_KEY_PREFIX = "index:";

  @PostConstruct
  public void init() {
    long maxTtl = config.getTokenStorage().getMaxTtl();
    int maxSize = config.getTokenStorage().getMaxSize();

    String configNamespace = config.getTokenStorage().getRedis().getNamespace();
    this.namespace = configNamespace.endsWith(":") ? configNamespace : configNamespace + ":";

    // Initialize L1 Caffeine Caches
    // We set a short TTL for L1 to ensure we eventually fetch fresh data from Redis
    // effectively mitigating consistency issues in a distributed setup.
    // 5 minutes or 1/10th of maxTtl, whichever is smaller, is a reasonable default for L1.
    long localTtl = Math.min(300, maxTtl > 0 ? maxTtl : 300);

    this.localAuthCache =
        Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(localTtl))
            .maximumSize(maxSize)
            .recordStats()
            .build();

    this.localIndexCache =
        Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(localTtl))
            .maximumSize(maxSize * 4L)
            .build();

    // Initialize L2 Redis Templates
    this.redisAuthTemplate = new RedisTemplate<>();
    this.redisAuthTemplate.setConnectionFactory(redisConnectionFactory);
    this.redisAuthTemplate.setKeySerializer(new StringRedisSerializer());
    // Use JDK Serialization for OAuth2Authorization to handle exact object state safely
    this.redisAuthTemplate.setValueSerializer(new JdkSerializationRedisSerializer());
    this.redisAuthTemplate.afterPropertiesSet();

    this.redisIndexTemplate = new RedisTemplate<>();
    this.redisIndexTemplate.setConnectionFactory(redisConnectionFactory);
    this.redisIndexTemplate.setKeySerializer(new StringRedisSerializer());
    this.redisIndexTemplate.setValueSerializer(new StringRedisSerializer());
    this.redisIndexTemplate.afterPropertiesSet();

    log.info(
        "Redis+Caffeine OAuth2AuthorizationService initialized. Namespace: {}, L1 TTL: {}s, L2 TTL: {}s",
        this.namespace,
        localTtl,
        maxTtl);
  }

  @Override
  public void save(OAuth2Authorization authorization) {
    String id = authorization.getId();
    Duration ttl = Duration.ofSeconds(config.getTokenStorage().getMaxTtl());

    // 1. Check for updates (Stale Index Cleanup Logic)
    // We check L1 first, then L2
    OAuth2Authorization existingAuth = localAuthCache.getIfPresent(id);
    if (existingAuth == null) {
      existingAuth = redisAuthTemplate.opsForValue().get(buildAuthKey(id));
    }

    if (existingAuth != null) {
      removeStaleIndexes(existingAuth, authorization);
    }

    // 2. Write to L2 (Redis)
    redisAuthTemplate.opsForValue().set(buildAuthKey(id), authorization, ttl);

    // 3. Write to L1 (Local)
    localAuthCache.put(id, authorization);

    // 4. Update Indexes (L1 + L2)
    updateIndexes(authorization, ttl);

    log.debug("💾 Saved authorization (Hybrid): id={}", id);
  }

  @Override
  public void remove(OAuth2Authorization authorization) {
    if (authorization == null) {
      return;
    }
    String id = authorization.getId();

    // 1. Invalidates L2
    redisAuthTemplate.delete(buildAuthKey(id));

    // 2. Invalidates L1
    localAuthCache.invalidate(id);

    // 3. Remove Indexes
    removeIndexes(authorization);

    log.debug("Removed authorization (Hybrid): id={}", id);
  }

  @Override
  public OAuth2Authorization findById(String id) {
    // 1. Check L1
    OAuth2Authorization auth = localAuthCache.getIfPresent(id);
    if (auth != null) {
      return auth;
    }

    // 2. Check L2
    auth = redisAuthTemplate.opsForValue().get(buildAuthKey(id));
    if (auth != null) {
      // Populate L1
      localAuthCache.put(id, auth);
    }

    return auth;
  }

  @Override
  public OAuth2Authorization findByToken(String token, OAuth2TokenType tokenType) {
    if (token == null) {
      return null;
    }

    String partialToken = token.substring(0, Math.min(10, token.length()));
    String indexKeySuffix = buildTokenIndexKeySuffix(tokenType, token);

    // 1. Check L1 Index
    String authId = localIndexCache.getIfPresent(indexKeySuffix);

    // 2. If Miss, Check L2 Index
    if (authId == null) {
      String redisKey = buildIndexKey(indexKeySuffix);
      authId = redisIndexTemplate.opsForValue().get(redisKey);
      if (authId != null) {
        // Populate L1 Index
        localIndexCache.put(indexKeySuffix, authId);
      }
    }

    if (authId == null) {
      log.warn(
          "❌ Token not found in index (Hybrid): type={}, token={}...",
          tokenType != null ? tokenType.getValue() : "null",
          partialToken);
      return null;
    }

    // 3. Fetch Authorization by ID (using our hybrid findById)
    OAuth2Authorization auth = findById(authId);

    if (auth == null) {
      // Verify consistency: Index exists but Data missing?
      log.warn("❌ Index valid but Authorization missing! authId={}", authId);
      // Clean up orphan index
      redisIndexTemplate.delete(buildIndexKey(indexKeySuffix));
      localIndexCache.invalidate(indexKeySuffix);
    } else {
      log.debug("✅ Found authorization (Hybrid) for token: {}...", partialToken);
    }

    return auth;
  }

  // --- Helper Methods ---

  private void updateIndexes(OAuth2Authorization authorization, Duration ttl) {
    String authId = authorization.getId();

    indexToken(authorization.getToken(OAuth2AccessToken.class), "access_token", authId, ttl);
    indexToken(authorization.getToken(OAuth2RefreshToken.class), "refresh_token", authId, ttl);
    indexToken(authorization.getToken("id_token"), "id_token", authId, ttl);

    OAuth2Authorization.Token<OAuth2AuthorizationCode> codeToken =
        authorization.getToken(OAuth2AuthorizationCode.class);
    if (codeToken != null) {
      indexTokenValue("code", codeToken.getToken().getTokenValue(), authId, ttl);
    }
  }

  private void indexToken(
      OAuth2Authorization.Token<?> token, String type, String authId, Duration ttl) {
    if (token != null && token.getToken() != null) {
      indexTokenValue(type, token.getToken().getTokenValue(), authId, ttl);
    }
  }

  private void indexTokenValue(String type, String tokenValue, String authId, Duration ttl) {
    String suffix = buildTokenIndexKeySuffix(type, tokenValue);

    // L2
    redisIndexTemplate.opsForValue().set(buildIndexKey(suffix), authId, ttl);

    // L1
    localIndexCache.put(suffix, authId);
  }

  private void removeStaleIndexes(OAuth2Authorization oldAuth, OAuth2Authorization newAuth) {
    // Check Refresh Token Rotation
    var oldRefresh = oldAuth.getRefreshToken();
    var newRefresh = newAuth.getRefreshToken();
    if (oldRefresh != null && newRefresh != null) {
      String oldVal = oldRefresh.getToken().getTokenValue();
      String newVal = newRefresh.getToken().getTokenValue();
      if (!oldVal.equals(newVal)) {
        removeIndex("refresh_token", oldVal);
        log.debug("🧹 Removed stale refresh token index (Hybrid): {}...", oldVal.substring(0, 10));
      }
    }

    // Check Access Token
    var oldAccess = oldAuth.getAccessToken();
    var newAccess = newAuth.getAccessToken();
    if (oldAccess != null && newAccess != null) {
      String oldVal = oldAccess.getToken().getTokenValue();
      String newVal = newAccess.getToken().getTokenValue();
      if (!oldVal.equals(newVal)) {
        removeIndex("access_token", oldVal);
      }
    }
  }

  private void removeIndexes(OAuth2Authorization authorization) {
    if (authorization.getAccessToken() != null) {
      removeIndex("access_token", authorization.getAccessToken().getToken().getTokenValue());
    }
    if (authorization.getRefreshToken() != null) {
      removeIndex("refresh_token", authorization.getRefreshToken().getToken().getTokenValue());
    }
    if (authorization.getToken("id_token") != null) {
      removeIndex("id_token", authorization.getToken("id_token").getToken().getTokenValue());
    }
    if (authorization.getToken(OAuth2AuthorizationCode.class) != null) {
      removeIndex(
          "code", authorization.getToken(OAuth2AuthorizationCode.class).getToken().getTokenValue());
    }
  }

  private void removeIndex(String type, String tokenValue) {
    String suffix = buildTokenIndexKeySuffix(type, tokenValue);
    redisIndexTemplate.delete(buildIndexKey(suffix));
    localIndexCache.invalidate(suffix);
  }

  private String buildAuthKey(String id) {
    return this.namespace + AUTH_KEY_PREFIX + id;
  }

  private String buildIndexKey(String suffix) {
    return this.namespace + INDEX_KEY_PREFIX + suffix;
  }

  private String buildTokenIndexKeySuffix(OAuth2TokenType type, String value) {
    return buildTokenIndexKeySuffix(type.getValue(), value);
  }

  private String buildTokenIndexKeySuffix(String typeValue, String value) {
    return typeValue + ":" + value;
  }
}
