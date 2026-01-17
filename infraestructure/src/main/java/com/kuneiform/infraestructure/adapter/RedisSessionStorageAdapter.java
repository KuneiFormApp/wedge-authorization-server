package com.kuneiform.infraestructure.adapter;

import com.kuneiform.domain.model.AuthorizationSession;
import com.kuneiform.domain.port.SessionStorage;
import com.kuneiform.infraestructure.config.properties.WedgeConfigProperties;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis implementation of SessionStorage using Spring Data Redis.
 *
 * <p>Active when {@code wedge.session.storage-type=redis}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "wedge.session.storage-type", havingValue = "redis")
public class RedisSessionStorageAdapter implements SessionStorage {

  private final RedisTemplate<String, AuthorizationSession> redisTemplate;
  private final org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate;
  private final WedgeConfigProperties properties;

  @PostConstruct
  public void init() {
    String namespace = properties.getSession().getRedis().getNamespace();
    long ttl = properties.getSession().getAuthTtl();
    log.info("Redis SessionStorage initialized: namespace={}, ttl={}s", namespace, ttl);
  }

  private String getKeyPrefix() {
    String namespace = properties.getSession().getRedis().getNamespace();
    return namespace.endsWith(":") ? namespace : namespace + ":";
  }

  private String getSessionKey(String authorizationCode) {
    return getKeyPrefix() + "session:" + authorizationCode;
  }

  private String getUserIndexKey(String userId) {
    return getKeyPrefix() + "user:" + userId;
  }

  private Duration getTtl() {
    return Duration.ofSeconds(properties.getSession().getAuthTtl());
  }

  @Override
  public void save(AuthorizationSession session) {
    try {
      String sessionKey = getSessionKey(session.getAuthorizationCode());
      String userIndexKey = getUserIndexKey(session.getUserId());
      Duration ttl = getTtl();

      // Save the session
      redisTemplate.opsForValue().set(sessionKey, session, ttl);

      // Add authorization code to user's session index set
      stringRedisTemplate.opsForSet().add(userIndexKey, session.getAuthorizationCode());
      // Set TTL on user index (refresh on each save to keep it alive while user has sessions)
      stringRedisTemplate.expire(userIndexKey, ttl);

      log.debug(
          "Saved session to Redis: code={}, userId={}, ttl={}s",
          session.getAuthorizationCode(),
          session.getUserId(),
          ttl.getSeconds());
    } catch (Exception e) {
      log.error("Failed to save session to Redis: code={}", session.getAuthorizationCode(), e);
      throw new RuntimeException("Failed to save session to Redis", e);
    }
  }

  @Override
  public Optional<AuthorizationSession> findByAuthorizationCode(String authorizationCode) {
    try {
      String key = getSessionKey(authorizationCode);
      AuthorizationSession session = redisTemplate.opsForValue().get(key);

      if (session == null) {
        return Optional.empty();
      }

      // Check if expired (Redis TTL handles this, but double-check)
      if (session.isExpired()) {
        log.debug("Session expired in Redis: code={}", authorizationCode);
        deleteByAuthorizationCode(authorizationCode);
        return Optional.empty();
      }

      return Optional.of(session);
    } catch (Exception e) {
      log.error("Failed to retrieve session from Redis: code={}", authorizationCode, e);
      return Optional.empty();
    }
  }

  @Override
  public java.util.List<AuthorizationSession> findByUserId(String userId) {
    try {
      String userIndexKey = getUserIndexKey(userId);
      java.util.Set<String> authCodes = stringRedisTemplate.opsForSet().members(userIndexKey);

      if (authCodes == null || authCodes.isEmpty()) {
        log.debug("No sessions found for user: {}", userId);
        return java.util.Collections.emptyList();
      }

      java.util.List<AuthorizationSession> userSessions = new java.util.ArrayList<>();
      java.util.List<String> expiredCodes = new java.util.ArrayList<>();

      for (String code : authCodes) {
        AuthorizationSession session = redisTemplate.opsForValue().get(getSessionKey(code));

        if (session == null) {
          // Session expired via TTL, mark for cleanup from index
          expiredCodes.add(code);
        } else if (session.isExpired()) {
          // Session marked as expired, clean it up
          expiredCodes.add(code);
          deleteByAuthorizationCode(code);
        } else if (session.getUserId().equals(userId)) {
          userSessions.add(session);
        }
      }

      // Clean up stale entries from the user index
      if (!expiredCodes.isEmpty()) {
        stringRedisTemplate.opsForSet().remove(userIndexKey, expiredCodes.toArray());
        log.debug("Cleaned {} stale entries from user index: {}", expiredCodes.size(), userId);
      }

      log.debug("Found {} sessions for user: {}", userSessions.size(), userId);
      return userSessions;
    } catch (Exception e) {
      log.error("Failed to find sessions by user ID in Redis: userId={}", userId, e);
      return java.util.Collections.emptyList();
    }
  }

  @Override
  public void deleteByAuthorizationCode(String authorizationCode) {
    try {
      String sessionKey = getSessionKey(authorizationCode);

      // Get the session first to find userId for index cleanup
      AuthorizationSession session = redisTemplate.opsForValue().get(sessionKey);

      // Delete the session
      redisTemplate.delete(sessionKey);

      // Remove from user index if we have the session info
      if (session != null) {
        String userIndexKey = getUserIndexKey(session.getUserId());
        stringRedisTemplate.opsForSet().remove(userIndexKey, authorizationCode);
      }

      log.debug("Deleted session from Redis: code={}", authorizationCode);
    } catch (Exception e) {
      log.error("Failed to delete session from Redis: code={}", authorizationCode, e);
    }
  }

  @Override
  public void deleteExpiredSessions() {
    // Redis TTL automatically handles expired keys
    // This method is a no-op for Redis implementation
    log.debug("Redis TTL handles session expiration automatically");
  }
}
