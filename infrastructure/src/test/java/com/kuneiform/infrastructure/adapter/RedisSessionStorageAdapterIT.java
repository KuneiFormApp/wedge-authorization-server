package com.kuneiform.infrastructure.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.kuneiform.domain.model.AuthorizationSession;
import com.kuneiform.infrastructure.config.RedisConfig;
import com.kuneiform.infrastructure.config.properties.WedgeConfigProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration tests for {@link RedisSessionStorageAdapter} using Testcontainers.
 *
 * <p>Tests the secondary index pattern where sessions are stored with keys like:
 *
 * <ul>
 *   <li>{@code {namespace}:session:{authCode}} - Session data
 *   <li>{@code {namespace}:user:{userId}} - Set of authorization codes for quick lookup
 * </ul>
 */
@SpringBootTest(
    classes = {RedisConfig.class, RedisSessionStorageAdapter.class},
    properties = {
      "wedge.session.storage-type=redis",
      "wedge.session.auth-ttl=60",
      "wedge.session.redis.namespace=wedge:test:session"
    })
@EnableConfigurationProperties(WedgeConfigProperties.class)
@Testcontainers
class RedisSessionStorageAdapterIT extends AbstractRedisIntegrationTest {

  @Autowired private RedisSessionStorageAdapter adapter;

  @Autowired private RedisTemplate<String, AuthorizationSession> redisTemplate;

  @Autowired private StringRedisTemplate stringRedisTemplate;

  @Autowired private WedgeConfigProperties properties;

  @BeforeEach
  void cleanUp() {
    // Clean all test keys before each test
    String pattern = properties.getSession().getRedis().getNamespace() + "*";
    Set<String> keys = stringRedisTemplate.keys(pattern);
    if (keys != null && !keys.isEmpty()) {
      stringRedisTemplate.delete(keys);
    }
  }

  // ==================== Basic CRUD Operations ====================

  @Test
  void shouldSaveAndRetrieveSession() {
    // Given
    AuthorizationSession session = createSession("code-123", "user-123");

    // When
    adapter.save(session);
    Optional<AuthorizationSession> retrieved = adapter.findByAuthorizationCode("code-123");

    // Then
    assertThat(retrieved).isPresent();
    assertThat(retrieved.get().getSessionId()).isEqualTo(session.getSessionId());
    assertThat(retrieved.get().getUserId()).isEqualTo("user-123");
    assertThat(retrieved.get().getClientId()).isEqualTo("test-client");
  }

  @Test
  void shouldReturnEmptyForNonExistentCode() {
    // When
    Optional<AuthorizationSession> result = adapter.findByAuthorizationCode("nonexistent-code");

    // Then
    assertThat(result).isEmpty();
  }

  @Test
  void shouldDeleteSession() {
    // Given
    AuthorizationSession session = createSession("code-delete", "user-delete");
    adapter.save(session);

    // Verify it exists
    assertThat(adapter.findByAuthorizationCode("code-delete")).isPresent();

    // When
    adapter.deleteByAuthorizationCode("code-delete");

    // Then
    assertThat(adapter.findByAuthorizationCode("code-delete")).isEmpty();
  }

  @Test
  void shouldReturnEmptyForExpiredSession() {
    // Given - session with expired domain timestamp
    AuthorizationSession expiredSession =
        AuthorizationSession.builder()
            .sessionId("session-expired")
            .authorizationCode("code-expired")
            .userId("user-expired")
            .clientId("test-client")
            .authorizedScopes(Set.of("openid"))
            .redirectUri("http://localhost:3000/callback")
            .createdAt(Instant.now().minusSeconds(1000))
            .expiresAt(Instant.now().minusSeconds(100)) // Already expired
            .build();

    adapter.save(expiredSession);

    // When
    Optional<AuthorizationSession> result = adapter.findByAuthorizationCode("code-expired");

    // Then
    assertThat(result).isEmpty();
  }

  // ==================== Secondary Index (findByUserId) ====================

  @Test
  void shouldFindSessionsByUserId() {
    // Given - multiple sessions for same user
    AuthorizationSession session1 = createSession("code-user1-a", "user-multi");
    AuthorizationSession session2 = createSession("code-user1-b", "user-multi");
    AuthorizationSession session3 = createSession("code-user2", "other-user");

    adapter.save(session1);
    adapter.save(session2);
    adapter.save(session3);

    // When
    List<AuthorizationSession> userSessions = adapter.findByUserId("user-multi");

    // Then
    assertThat(userSessions).hasSize(2);
    assertThat(userSessions)
        .extracting(AuthorizationSession::getAuthorizationCode)
        .containsExactlyInAnyOrder("code-user1-a", "code-user1-b");
  }

  @Test
  void shouldReturnEmptyListForUserWithNoSessions() {
    // When
    List<AuthorizationSession> result = adapter.findByUserId("user-no-sessions");

    // Then
    assertThat(result).isEmpty();
  }

  @Test
  void shouldCleanupStaleIndexEntriesOnFindByUserId() {
    // Given - save session then manually delete the session key (simulating TTL expiry)
    AuthorizationSession session = createSession("code-stale", "user-stale");
    adapter.save(session);

    // Manually delete the session key (simulating Redis TTL expiry)
    String sessionKey = getSessionKey("code-stale");
    redisTemplate.delete(sessionKey);

    // Verify session is gone but index still has the entry
    String userIndexKey = getUserIndexKey("user-stale");
    assertThat(stringRedisTemplate.opsForSet().isMember(userIndexKey, "code-stale")).isTrue();

    // When - findByUserId should trigger cleanup
    List<AuthorizationSession> result = adapter.findByUserId("user-stale");

    // Then - should return empty list and clean up the stale index entry
    assertThat(result).isEmpty();
    assertThat(stringRedisTemplate.opsForSet().isMember(userIndexKey, "code-stale")).isFalse();
  }

  // ==================== Delete with Index Cleanup ====================

  @Test
  void shouldRemoveFromUserIndexOnDelete() {
    // Given
    AuthorizationSession session = createSession("code-index-delete", "user-index");
    adapter.save(session);

    // Verify index has the entry
    String userIndexKey = getUserIndexKey("user-index");
    assertThat(stringRedisTemplate.opsForSet().isMember(userIndexKey, "code-index-delete"))
        .isTrue();

    // When
    adapter.deleteByAuthorizationCode("code-index-delete");

    // Then - both session and index entry should be gone
    assertThat(adapter.findByAuthorizationCode("code-index-delete")).isEmpty();
    assertThat(stringRedisTemplate.opsForSet().isMember(userIndexKey, "code-index-delete"))
        .isFalse();
  }

  // ==================== TTL Verification ====================

  @Test
  void shouldSetTtlOnSessionKey() {
    // Given
    AuthorizationSession session = createSession("code-ttl", "user-ttl");

    // When
    adapter.save(session);

    // Then - verify TTL is set (should be close to configured 60 seconds)
    String sessionKey = getSessionKey("code-ttl");
    Long ttl = redisTemplate.getExpire(sessionKey);

    assertThat(ttl).isNotNull();
    assertThat(ttl).isGreaterThan(0);
    assertThat(ttl).isLessThanOrEqualTo(60);
  }

  @Test
  void shouldSetTtlOnUserIndex() {
    // Given
    AuthorizationSession session = createSession("code-idx-ttl", "user-idx-ttl");

    // When
    adapter.save(session);

    // Then - verify TTL is set on user index
    String userIndexKey = getUserIndexKey("user-idx-ttl");
    Long ttl = stringRedisTemplate.getExpire(userIndexKey);

    assertThat(ttl).isNotNull();
    assertThat(ttl).isGreaterThan(0);
    assertThat(ttl).isLessThanOrEqualTo(60);
  }

  @Test
  void shouldRefreshUserIndexTtlOnSubsequentSaves() throws InterruptedException {
    // Given - save first session
    AuthorizationSession session1 = createSession("code-refresh-1", "user-refresh");
    adapter.save(session1);

    String userIndexKey = getUserIndexKey("user-refresh");
    Long initialTtl = stringRedisTemplate.getExpire(userIndexKey);

    // Wait a bit
    Thread.sleep(1000);

    // When - save another session for same user
    AuthorizationSession session2 = createSession("code-refresh-2", "user-refresh");
    adapter.save(session2);

    // Then - TTL should be refreshed (reset to full duration)
    Long refreshedTtl = stringRedisTemplate.getExpire(userIndexKey);
    assertThat(refreshedTtl).isGreaterThanOrEqualTo(initialTtl);
  }

  // ==================== Key Structure Verification ====================

  @Test
  void shouldUseCorrectKeyStructure() {
    // Given
    AuthorizationSession session = createSession("code-key-test", "user-key-test");

    // When
    adapter.save(session);

    // Then - verify key structure
    String namespace = properties.getSession().getRedis().getNamespace();
    String expectedSessionKey = namespace + ":session:code-key-test";
    String expectedUserIndexKey = namespace + ":user:user-key-test";

    assertThat(redisTemplate.hasKey(expectedSessionKey)).isTrue();
    assertThat(stringRedisTemplate.hasKey(expectedUserIndexKey)).isTrue();
  }

  // ==================== Helper Methods ====================

  private AuthorizationSession createSession(String authCode, String userId) {
    return AuthorizationSession.builder()
        .sessionId("session-" + authCode)
        .authorizationCode(authCode)
        .userId(userId)
        .clientId("test-client")
        .authorizedScopes(Set.of("openid", "profile"))
        .redirectUri("http://localhost:3000/callback")
        .createdAt(Instant.now())
        .expiresAt(Instant.now().plus(Duration.ofMinutes(10)))
        .build();
  }

  private String getSessionKey(String authCode) {
    String namespace = properties.getSession().getRedis().getNamespace();
    return (namespace.endsWith(":") ? namespace : namespace + ":") + "session:" + authCode;
  }

  private String getUserIndexKey(String userId) {
    String namespace = properties.getSession().getRedis().getNamespace();
    return (namespace.endsWith(":") ? namespace : namespace + ":") + "user:" + userId;
  }
}
