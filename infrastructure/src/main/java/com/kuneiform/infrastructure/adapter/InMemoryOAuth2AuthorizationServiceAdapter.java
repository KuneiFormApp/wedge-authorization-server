package com.kuneiform.infrastructure.adapter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.kuneiform.infrastructure.config.properties.WedgeConfigProperties;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.stereotype.Component;

/**
 * Caffeine-based in-memory implementation of OAuth2AuthorizationService with custom per-item TTL.
 *
 * <p>Uses custom {@link Expiry} policy to calculate TTL based on refresh token expiration time.
 * Suitable for development and single-instance deployments.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "wedge.token-storage.type",
    havingValue = "in-memory",
    matchIfMissing = true)
public class InMemoryOAuth2AuthorizationServiceAdapter implements OAuth2AuthorizationService {

  private final WedgeConfigProperties config;

  private Cache<String, OAuth2Authorization> authorizationCache;
  private Cache<String, String> tokenIndexCache; // token value -> authorization ID
  private Cache<String, String> principalIndexCache; // userId -> authorization ID

  @PostConstruct
  public void init() {
    long maxTtl = config.getTokenStorage().getMaxTtl();
    int maxSize = config.getTokenStorage().getMaxSize();

    if (maxTtl <= 0) {
      throw new IllegalArgumentException("Token storage max-ttl must be positive, got: " + maxTtl);
    }

    if (maxSize <= 0) {
      throw new IllegalArgumentException(
          "Token storage max-size must be positive, got: " + maxSize);
    }

    // Main cache with custom per-item TTL
    this.authorizationCache =
        Caffeine.newBuilder()
            .expireAfter(new OAuth2AuthorizationExpiry(config))
            .maximumSize(maxSize)
            .recordStats()
            .build();

    // Index cache for fast token lookups (token value -> auth ID)
    this.tokenIndexCache =
        Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(maxTtl))
            .maximumSize(maxSize * 4) // Up to 4 tokens per authorization
            .build();

    // Principal index cache for logout support (userId -> auth ID)
    this.principalIndexCache =
        Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(maxTtl))
            .maximumSize(maxSize)
            .build();

    log.info(
        "In-memory OAuth2AuthorizationService initialized: max-ttl={}s, maxSize={}",
        maxTtl,
        maxSize);
  }

  @Override
  public void save(OAuth2Authorization authorization) {
    String id = authorization.getId();

    OAuth2Authorization existingAuth = authorizationCache.getIfPresent(id);
    boolean isUpdate = existingAuth != null;
    log.debug("SAVE called for authId={} ({})", id, isUpdate ? "UPDATE" : "CREATE");

    if (isUpdate) {
      // Clean up old indexes if tokens have changed (e.g. Refresh Token Rotation)
      removeStaleTokenIndexes(existingAuth, authorization);
    }

    // Store authorization by ID
    authorizationCache.put(id, authorization);

    // Create indexes for fast token lookup
    createTokenIndexes(authorization);

    // Index by userId for logout support
    // Note: getPrincipalName() returns the userId from the User object
    if (authorization.getPrincipalName() != null) {
      principalIndexCache.put(authorization.getPrincipalName(), id);
    }

    log.debug("Saved authorization: id={}, principal={}", id, authorization.getPrincipalName());
  }

  @Override
  public void remove(OAuth2Authorization authorization) {
    if (authorization == null) {
      return;
    }

    String id = authorization.getId();
    authorizationCache.invalidate(id);
    removeTokenIndexes(authorization);

    // Remove principal index
    if (authorization.getPrincipalName() != null) {
      principalIndexCache.invalidate(authorization.getPrincipalName());
    }

    log.debug("Removed authorization: id={}", id);
  }

  @Override
  public OAuth2Authorization findById(String id) {
    return authorizationCache.getIfPresent(id);
  }

  @Override
  public OAuth2Authorization findByToken(String token, OAuth2TokenType tokenType) {
    if (token == null) {
      return null;
    }

    log.debug(
        "Looking for token: type={}, value={}",
        tokenType != null ? tokenType.getValue() : "null",
        token.substring(0, Math.min(20, token.length())) + "...");

    // Lookup via index
    String indexKey = buildTokenIndexKey(tokenType, token);
    String authId = tokenIndexCache.getIfPresent(indexKey);

    if (authId == null) {
      log.warn("Token NOT found in index: key={}", indexKey);
      return null;
    }

    log.debug("Found auth ID in index: {}", authId);

    // Get from main cache
    OAuth2Authorization auth = authorizationCache.getIfPresent(authId);

    if (auth == null) {
      log.warn("Authorization NOT found in cache for ID: {}", authId);
    } else {
      log.debug("Found authorization in cache");
      // Debug: Compare requested token with current stored token
      if (tokenType != null && OAuth2TokenType.REFRESH_TOKEN.equals(tokenType)) {
        var currentRefresh = auth.getRefreshToken();
        if (currentRefresh != null) {
          String currentVal = currentRefresh.getToken().getTokenValue();
          boolean match = currentVal.equals(token);
          log.debug(
              "Reuse Check: Requested={}, CurrentStored={} (Match={})",
              token.substring(0, Math.min(10, token.length())) + "...",
              currentVal.substring(0, Math.min(10, currentVal.length())) + "...",
              match);
        } else {
          log.warn("Reuse Check: No Refresh Token in stored authorization!");
        }
      }
    }

    return auth;
  }

  private void createTokenIndexes(OAuth2Authorization authorization) {
    String authId = authorization.getId();

    // Index all token types using class-based keys (Spring stores tokens by class,
    // not string keys)
    indexToken(authorization.getToken(OAuth2AccessToken.class), "access_token", authId);
    indexToken(authorization.getToken(OAuth2RefreshToken.class), "refresh_token", authId);
    indexToken(authorization.getToken("id_token"), "id_token", authId);

    OAuth2Authorization.Token<OAuth2AuthorizationCode> codeToken =
        authorization.getToken(OAuth2AuthorizationCode.class);

    if (codeToken != null) {
      String tokenValue = codeToken.getToken().getTokenValue();
      // Only index if not already present to avoid spamming logs
      if (tokenIndexCache.getIfPresent(buildTokenIndexKey("code", tokenValue)) == null) {
        log.debug(
            "Indexed authorization code: {}",
            tokenValue.substring(0, Math.min(20, tokenValue.length())) + "...");
        String indexKey = buildTokenIndexKey("code", tokenValue);
        tokenIndexCache.put(indexKey, authId);
      }
    }

    // Index STATE token for consent flow (Spring Auth Server looks up by state)
    String state = authorization.getAttribute("state");
    if (state != null) {
      String indexKey = buildTokenIndexKey("state", state);
      tokenIndexCache.put(indexKey, authId);
      log.debug("Indexed state token");
    }
  }

  private void removeStaleTokenIndexes(OAuth2Authorization oldAuth, OAuth2Authorization newAuth) {
    // Check Refresh Token change
    var oldRefresh = oldAuth.getRefreshToken();
    var newRefresh = newAuth.getRefreshToken();
    if (oldRefresh != null && newRefresh != null) {
      String oldVal = oldRefresh.getToken().getTokenValue();
      String newVal = newRefresh.getToken().getTokenValue();
      if (!oldVal.equals(newVal)) {
        String key = buildTokenIndexKey("refresh_token", oldVal);
        tokenIndexCache.invalidate(key);
        log.debug(
            "Removed stale refresh token index: {}",
            key.substring(0, Math.min(30, key.length())) + "...");
      }
    }

    // Check Access Token change
    var oldAccess = oldAuth.getAccessToken();
    var newAccess = newAuth.getAccessToken();
    if (oldAccess != null && newAccess != null) {
      String oldVal = oldAccess.getToken().getTokenValue();
      String newVal = newAccess.getToken().getTokenValue();
      if (!oldVal.equals(newVal)) {
        String key = buildTokenIndexKey("access_token", oldVal);
        tokenIndexCache.invalidate(key);
      }
    }
  }

  private void indexToken(
      OAuth2Authorization.Token<?> token, String tokenTypeValue, String authId) {
    if (token != null && token.getToken() != null) {
      String tokenValue = token.getToken().getTokenValue();
      String indexKey = buildTokenIndexKey(tokenTypeValue, tokenValue);
      tokenIndexCache.put(indexKey, authId);
      log.debug(
          "Indexed token: type={}, key={}, authId={}",
          tokenTypeValue,
          indexKey.substring(0, Math.min(30, indexKey.length())) + "...",
          authId);
    }
  }

  private void removeTokenIndexes(OAuth2Authorization authorization) {
    // Remove all token indexes when authorization is removed
    if (authorization.getAccessToken() != null) {
      tokenIndexCache.invalidate(
          buildTokenIndexKey(
              "access_token", authorization.getAccessToken().getToken().getTokenValue()));
    }

    if (authorization.getRefreshToken() != null) {
      tokenIndexCache.invalidate(
          buildTokenIndexKey(
              "refresh_token", authorization.getRefreshToken().getToken().getTokenValue()));
    }

    OAuth2Authorization.Token<?> idToken = authorization.getToken("id_token");
    if (idToken != null) {
      tokenIndexCache.invalidate(
          buildTokenIndexKey("id_token", idToken.getToken().getTokenValue()));
    }

    OAuth2Authorization.Token<OAuth2AuthorizationCode> authCode =
        authorization.getToken(OAuth2AuthorizationCode.class);
    if (authCode != null) {
      tokenIndexCache.invalidate(buildTokenIndexKey("code", authCode.getToken().getTokenValue()));
    }

    // Remove state index
    String state = authorization.getAttribute("state");
    if (state != null) {
      tokenIndexCache.invalidate(buildTokenIndexKey("state", state));
    }
  }

  private String buildTokenIndexKey(String tokenTypeValue, String tokenValue) {
    return tokenTypeValue + ":" + tokenValue;
  }

  private String buildTokenIndexKey(OAuth2TokenType tokenType, String tokenValue) {
    return tokenType.getValue() + ":" + tokenValue;
  }

  /** Returns the current number of authorizations in cache. */
  public long getAuthorizationCount() {
    return authorizationCache.estimatedSize();
  }

  public String getStats() {
    return "Authorizations: "
        + authorizationCache.stats()
        + ", Indexes: "
        + tokenIndexCache.stats();
  }

  /** Used during logout to find and revoke authorizations for a specific user. */
  public OAuth2Authorization findByUserId(String userId) {
    if (userId == null) {
      return null;
    }

    String authId = principalIndexCache.getIfPresent(userId);
    if (authId == null) {
      return null;
    }

    return authorizationCache.getIfPresent(authId);
  }

  /**
   * Custom Caffeine Expiry policy that calculates TTL per authorization based on the refresh
   * token's remaining lifetime.
   */
  private static class OAuth2AuthorizationExpiry implements Expiry<String, OAuth2Authorization> {

    private final WedgeConfigProperties config;

    public OAuth2AuthorizationExpiry(WedgeConfigProperties config) {
      this.config = config;
    }

    @Override
    public long expireAfterCreate(String key, OAuth2Authorization value, long currentTime) {
      return calculateExpirationNanos(value);
    }

    @Override
    public long expireAfterUpdate(
        String key, OAuth2Authorization value, long currentTime, long currentDuration) {
      // Recalculate on update (e.g., when token is rotated)
      return calculateExpirationNanos(value);
    }

    @Override
    public long expireAfterRead(
        String key, OAuth2Authorization value, long currentTime, long currentDuration) {
      // Don't change expiration on read
      return currentDuration;
    }

    private long calculateExpirationNanos(OAuth2Authorization authorization) {
      // Find the earliest expiry time across ALL tokens
      Instant earliestExpiry = null;

      // Check authorization code
      OAuth2Authorization.Token<?> code = authorization.getToken("code");
      if (code != null && code.getToken() != null && code.getToken().getExpiresAt() != null) {
        earliestExpiry = code.getToken().getExpiresAt();
      }

      // Check access token
      OAuth2Authorization.Token<?> accessToken = authorization.getAccessToken();
      if (accessToken != null
          && accessToken.getToken() != null
          && accessToken.getToken().getExpiresAt() != null) {
        if (earliestExpiry == null
            || accessToken.getToken().getExpiresAt().isBefore(earliestExpiry)) {
          earliestExpiry = accessToken.getToken().getExpiresAt();
        }
      }

      // Check refresh token
      OAuth2Authorization.Token<?> refreshToken = authorization.getRefreshToken();
      if (refreshToken != null
          && refreshToken.getToken() != null
          && refreshToken.getToken().getExpiresAt() != null) {
        if (earliestExpiry == null
            || refreshToken.getToken().getExpiresAt().isBefore(earliestExpiry)) {
          earliestExpiry = refreshToken.getToken().getExpiresAt();
        }
      }

      // Check ID token
      OAuth2Authorization.Token<?> idToken = authorization.getToken("id_token");
      if (idToken != null
          && idToken.getToken() != null
          && idToken.getToken().getExpiresAt() != null) {
        if (earliestExpiry == null || idToken.getToken().getExpiresAt().isBefore(earliestExpiry)) {
          earliestExpiry = idToken.getToken().getExpiresAt();
        }
      }

      if (earliestExpiry != null) {
        Duration remaining = Duration.between(Instant.now(), earliestExpiry);

        if (remaining.isNegative()) {
          // Already expired
          return 0;
        }

        // Cap at max-ttl
        Duration maxTtl = Duration.ofSeconds(config.getTokenStorage().getMaxTtl());
        Duration actual = remaining.compareTo(maxTtl) > 0 ? maxTtl : remaining;

        return actual.toNanos();
      }

      // Fallback to max-ttl if no tokens with expiry
      return Duration.ofSeconds(config.getTokenStorage().getMaxTtl()).toNanos();
    }
  }
}
