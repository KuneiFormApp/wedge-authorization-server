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

  private Duration getTtl() {
    return Duration.ofSeconds(properties.getSession().getAuthTtl());
  }

  @Override
  public void save(AuthorizationSession session) {
    try {
      String key = getKeyPrefix() + session.getAuthorizationCode();
      redisTemplate.opsForValue().set(key, session, getTtl());

      log.debug(
          "Saved session to Redis: code={}, userId={}, ttl={}s",
          session.getAuthorizationCode(),
          session.getUserId(),
          getTtl().getSeconds());
    } catch (Exception e) {
      log.error("Failed to save session to Redis: code={}", session.getAuthorizationCode(), e);
      throw new RuntimeException("Failed to save session to Redis", e);
    }
  }

  @Override
  public Optional<AuthorizationSession> findByAuthorizationCode(String authorizationCode) {
    try {
      String key = getKeyPrefix() + authorizationCode;
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
  public void deleteByAuthorizationCode(String authorizationCode) {
    try {
      String key = getKeyPrefix() + authorizationCode;
      redisTemplate.delete(key);
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
