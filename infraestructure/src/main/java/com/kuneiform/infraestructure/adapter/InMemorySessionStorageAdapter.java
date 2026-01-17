package com.kuneiform.infraestructure.adapter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.kuneiform.domain.model.AuthorizationSession;
import com.kuneiform.domain.port.SessionStorage;
import com.kuneiform.infraestructure.config.properties.WedgeConfigProperties;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Caffeine-based in-memory implementation of SessionStorage.
 *
 * <p>Uses Caffeine cache for automatic TTL-based expiration, size limits, and high-performance
 * concurrent access. Suitable for development and single-instance deployments.
 */
@Slf4j
@Component
@ConditionalOnProperty(
    name = "wedge.session.storage-type",
    havingValue = "in-memory",
    matchIfMissing = true)
public class InMemorySessionStorageAdapter implements SessionStorage {

  private final WedgeConfigProperties properties;
  private Cache<String, AuthorizationSession> sessions;

  public InMemorySessionStorageAdapter(WedgeConfigProperties properties) {
    this.properties = properties;
  }

  @PostConstruct
  public void init() {
    long ttl = properties.getSession().getAuthTtl();
    int maxSize = properties.getSession().getMaxSize();

    if (ttl <= 0) {
      throw new IllegalArgumentException("Session TTL must be positive, got: " + ttl);
    }

    if (maxSize <= 0) {
      throw new IllegalArgumentException("Session max size must be positive, got: " + maxSize);
    }

    this.sessions =
        Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(ttl))
            .maximumSize(maxSize)
            .recordStats()
            .removalListener(
                (key, value, cause) -> {
                  if (cause.wasEvicted()) {
                    log.debug("Session evicted: code={}, cause={}", key, cause);
                  }
                })
            .build();

    log.info("Caffeine-based SessionStorage initialized: ttl={}s, maxSize={}", ttl, maxSize);
  }

  @Override
  public void save(AuthorizationSession session) {
    sessions.put(session.getAuthorizationCode(), session);

    log.debug(
        "Saved session: code={}, userId={}", session.getAuthorizationCode(), session.getUserId());
  }

  @Override
  public Optional<AuthorizationSession> findByAuthorizationCode(String authorizationCode) {
    AuthorizationSession session = sessions.getIfPresent(authorizationCode);

    if (session == null) {
      return Optional.empty();
    }

    if (session.isExpired()) {
      log.debug("Session expired (domain): code={}", authorizationCode);
      sessions.invalidate(authorizationCode);
      return Optional.empty();
    }

    return Optional.of(session);
  }

  @Override
  public java.util.List<AuthorizationSession> findByUserId(String userId) {
    return sessions.asMap().values().stream()
        .filter(session -> session.getUserId().equals(userId))
        .filter(session -> !session.isExpired())
        .toList();
  }

  @Override
  public void deleteByAuthorizationCode(String authorizationCode) {
    sessions.invalidate(authorizationCode);
    log.debug("Deleted session: code={}", authorizationCode);
  }

  @Override
  public void deleteExpiredSessions() {
    // Caffeine handles TTL-based expiration automatically
    // This triggers any pending maintenance operations
    sessions.cleanUp();

    long estimatedSize = sessions.estimatedSize();
    log.debug("Session cleanup completed. Current size: {}", estimatedSize);
  }

  /** Returns the current number of sessions in cache. */
  public long getSessionCount() {
    return sessions.estimatedSize();
  }

  /** Returns cache statistics if recording is enabled. */
  public String getStats() {
    return sessions.stats().toString();
  }
}
