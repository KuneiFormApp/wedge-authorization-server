package com.kuneiform.infraestructure.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.kuneiform.domain.model.AuthorizationSession;
import com.kuneiform.infraestructure.config.RedisConfig;
import com.kuneiform.infraestructure.config.properties.WedgeConfigProperties;
import com.redis.testcontainers.RedisContainer;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(
    classes = {
      RedisSessionStorageAdapterIntegrationTest.TestConfig.class,
      RedisSessionStorageAdapter.class,
      RedisConfig.class
    })
@EnableConfigurationProperties(WedgeConfigProperties.class)
@Testcontainers
class RedisSessionStorageAdapterIntegrationTest {

  @TestConfiguration
  static class TestConfig {
    @Bean
    public ObjectMapper objectMapper() {
      return JsonMapper.builder()
          .enable(tools.jackson.databind.MapperFeature.INFER_CREATOR_FROM_CONSTRUCTOR_PROPERTIES)
          .build();
    }
  }

  @Container
  static RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:7-alpine"));

  @DynamicPropertySource
  static void redisProperties(DynamicPropertyRegistry registry) {
    registry.add("wedge.session.storage-type", () -> "redis");
    registry.add("wedge.session.redis.host", redis::getHost);
    registry.add("wedge.session.redis.port", redis::getRedisPort);
    registry.add("wedge.session.redis.namespace", () -> "wedge:test");
    registry.add("wedge.session.ttl", () -> "60");
    // Overwrite any defaults to empty for Testcontainers (which has no password by default)
    registry.add("wedge.session.redis.username", () -> "");
    registry.add("wedge.session.redis.password", () -> "");
  }

  @Autowired private RedisSessionStorageAdapter adapter;

  @Autowired private RedisTemplate<String, AuthorizationSession> redisTemplate;

  @BeforeEach
  void setUp() {
    if (redisTemplate != null) {
      redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }
  }

  // ========== save() Tests ==========

  @Test
  @DisplayName("Should save session to Redis with correct key")
  void shouldSaveSession() {
    String code = UUID.randomUUID().toString();
    AuthorizationSession session =
        AuthorizationSession.builder()
            .authorizationCode(code)
            .userId("test-user")
            .createdAt(Instant.now())
            .build();

    adapter.save(session);

    String expectedKey = "wedge:test:" + code;
    assertThat(redisTemplate.hasKey(expectedKey)).isTrue();
  }

  @Test
  @DisplayName("Should save session with all fields correctly")
  void shouldSaveSessionWithAllFields() {
    String code = UUID.randomUUID().toString();
    Instant now = Instant.now();
    AuthorizationSession session =
        AuthorizationSession.builder()
            .authorizationCode(code)
            .userId("test-user-123")
            .clientId("test-client")
            .redirectUri("https://example.com/callback")
            .authorizedScopes(Set.of("read", "write"))
            .state("test-state")
            .codeChallenge("test-challenge")
            .codeChallengeMethod("S256")
            .createdAt(now)
            .build();

    adapter.save(session);

    String key = "wedge:test:" + code;
    AuthorizationSession retrieved = redisTemplate.opsForValue().get(key);

    assertThat(retrieved).isNotNull();
    assertThat(retrieved.getAuthorizationCode()).isEqualTo(code);
    assertThat(retrieved.getUserId()).isEqualTo("test-user-123");
    assertThat(retrieved.getClientId()).isEqualTo("test-client");
    assertThat(retrieved.getRedirectUri()).isEqualTo("https://example.com/callback");
    assertThat(retrieved.getAuthorizedScopes()).contains("read", "write");
    assertThat(retrieved.getState()).isEqualTo("test-state");
    assertThat(retrieved.getCodeChallenge()).isEqualTo("test-challenge");
    assertThat(retrieved.getCodeChallengeMethod()).isEqualTo("S256");
    assertThat(retrieved.getCreatedAt()).isEqualTo(now);
  }

  @Test
  @DisplayName("Should save session with TTL set correctly")
  void shouldSaveSessionWithTtl() throws InterruptedException {
    String code = UUID.randomUUID().toString();
    AuthorizationSession session =
        AuthorizationSession.builder()
            .authorizationCode(code)
            .userId("test-user")
            .createdAt(Instant.now())
            .build();

    adapter.save(session);

    String key = "wedge:test:" + code;
    Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);

    assertThat(ttl).isNotNull();
    assertThat(ttl).isGreaterThan(55L).isLessThanOrEqualTo(60L); // Should be close to 60 seconds
  }

  @Test
  @DisplayName("Should overwrite existing session when saving with same code")
  void shouldOverwriteExistingSession() {
    String code = UUID.randomUUID().toString();

    AuthorizationSession session1 =
        AuthorizationSession.builder()
            .authorizationCode(code)
            .userId("user-1")
            .createdAt(Instant.now())
            .build();

    adapter.save(session1);

    AuthorizationSession session2 =
        AuthorizationSession.builder()
            .authorizationCode(code)
            .userId("user-2")
            .createdAt(Instant.now())
            .build();

    adapter.save(session2);

    Optional<AuthorizationSession> retrieved = adapter.findByAuthorizationCode(code);

    assertThat(retrieved).isPresent();
    assertThat(retrieved.get().getUserId()).isEqualTo("user-2");
  }

  // ========== findByAuthorizationCode() Tests ==========

  @Test
  @DisplayName("Should find session by authorization code")
  void shouldFindSessionByCode() {
    String code = UUID.randomUUID().toString();
    AuthorizationSession session =
        AuthorizationSession.builder()
            .authorizationCode(code)
            .userId("test-user")
            .createdAt(Instant.now())
            .build();

    adapter.save(session);

    Optional<AuthorizationSession> found = adapter.findByAuthorizationCode(code);

    assertThat(found).isPresent();
    assertThat(found.get().getAuthorizationCode()).isEqualTo(code);
    assertThat(found.get().getUserId()).isEqualTo("test-user");
  }

  @Test
  @DisplayName("Should return empty optional when session not found")
  void shouldReturnEmptyWhenSessionNotFound() {
    String nonExistentCode = UUID.randomUUID().toString();

    Optional<AuthorizationSession> found = adapter.findByAuthorizationCode(nonExistentCode);

    assertThat(found).isEmpty();
  }

  @Test
  @DisplayName("Should return empty optional and delete when session is expired")
  void shouldReturnEmptyWhenSessionExpired() {
    String code = UUID.randomUUID().toString();

    // Create an expired session (created 6 minutes ago, expired 1 minute ago)
    AuthorizationSession expiredSession =
        AuthorizationSession.builder()
            .authorizationCode(code)
            .userId("test-user")
            .createdAt(Instant.now().minus(Duration.ofMinutes(6)))
            .expiresAt(Instant.now().minus(Duration.ofMinutes(1)))
            .build();

    // Save to Redis directly
    String key = "wedge:test:" + code;
    redisTemplate.opsForValue().set(key, expiredSession, Duration.ofSeconds(60));

    // Try to find the expired session
    Optional<AuthorizationSession> found = adapter.findByAuthorizationCode(code);

    assertThat(found).isEmpty();
    // Session should also be deleted from Redis
    assertThat(redisTemplate.hasKey(key)).isFalse();
  }

  @Test
  @DisplayName("Should handle null authorization code gracefully")
  void shouldHandleNullAuthorizationCode() {
    Optional<AuthorizationSession> found = adapter.findByAuthorizationCode(null);
    assertThat(found).isEmpty();
  }

  @Test
  @DisplayName("Should handle empty authorization code gracefully")
  void shouldHandleEmptyAuthorizationCode() {
    Optional<AuthorizationSession> found = adapter.findByAuthorizationCode("");
    assertThat(found).isEmpty();
  }

  // ========== deleteByAuthorizationCode() Tests ==========

  @Test
  @DisplayName("Should delete session by authorization code")
  void shouldDeleteSessionByCode() {
    String code = UUID.randomUUID().toString();
    AuthorizationSession session =
        AuthorizationSession.builder()
            .authorizationCode(code)
            .userId("test-user")
            .createdAt(Instant.now())
            .build();

    adapter.save(session);
    String key = "wedge:test:" + code;
    assertThat(redisTemplate.hasKey(key)).isTrue();

    adapter.deleteByAuthorizationCode(code);

    assertThat(redisTemplate.hasKey(key)).isFalse();
    assertThat(adapter.findByAuthorizationCode(code)).isEmpty();
  }

  @Test
  @DisplayName("Should handle deletion of non-existent session gracefully")
  void shouldHandleDeleteNonExistentSession() {
    String nonExistentCode = UUID.randomUUID().toString();

    // Should not throw exception
    adapter.deleteByAuthorizationCode(nonExistentCode);
  }

  @Test
  @DisplayName("Should handle null code in deletion gracefully")
  void shouldHandleDeleteNullCode() {
    // Should not throw exception
    adapter.deleteByAuthorizationCode(null);
  }

  // ========== deleteExpiredSessions() Tests ==========

  @Test
  @DisplayName("Should handle deleteExpiredSessions as no-op")
  void shouldHandleDeleteExpiredSessionsAsNoOp() {
    // Create some sessions
    for (int i = 0; i < 5; i++) {
      String code = UUID.randomUUID().toString();
      AuthorizationSession session =
          AuthorizationSession.builder()
              .authorizationCode(code)
              .userId("user-" + i)
              .createdAt(Instant.now())
              .build();
      adapter.save(session);
    }

    // Call deleteExpiredSessions (should be a no-op for Redis)
    adapter.deleteExpiredSessions();

    // All sessions should still exist (deleteExpiredSessions doesn't actually
    // delete in Redis)
    // This is expected behavior as Redis handles expiration automatically
  }

  // ========== Namespace Tests ==========

  @Test
  @DisplayName("Should use correct namespace prefix for keys")
  void shouldUseCorrectNamespacePrefix() {
    String code = UUID.randomUUID().toString();
    AuthorizationSession session =
        AuthorizationSession.builder()
            .authorizationCode(code)
            .userId("test-user")
            .createdAt(Instant.now())
            .build();

    adapter.save(session);

    String expectedKey = "wedge:test:" + code;
    assertThat(redisTemplate.hasKey(expectedKey)).isTrue();

    // Verify wrong namespace doesn't find it
    String wrongKey = "wrong:namespace:" + code;
    assertThat(redisTemplate.hasKey(wrongKey)).isFalse();
  }

  // ========== Concurrent Operations Tests ==========

  @Test
  @DisplayName("Should handle concurrent saves correctly")
  void shouldHandleConcurrentSaves() throws InterruptedException {
    int threadCount = 10;
    CountDownLatch latch = new CountDownLatch(threadCount);

    for (int i = 0; i < threadCount; i++) {
      final int userId = i;
      new Thread(
              () -> {
                String code = UUID.randomUUID().toString();
                AuthorizationSession session =
                    AuthorizationSession.builder()
                        .authorizationCode(code)
                        .userId("user-" + userId)
                        .createdAt(Instant.now())
                        .build();
                adapter.save(session);
                latch.countDown();
              })
          .start();
    }

    latch.await(5, TimeUnit.SECONDS);

    // Verify all sessions were saved (count keys with our namespace)
    var keys = redisTemplate.keys("wedge:test:*");
    assertThat(keys).isNotNull();
    assertThat(keys).hasSize(threadCount);
  }

  @Test
  @DisplayName("Should handle concurrent reads correctly")
  void shouldHandleConcurrentReads() throws InterruptedException {
    String code = UUID.randomUUID().toString();
    AuthorizationSession session =
        AuthorizationSession.builder()
            .authorizationCode(code)
            .userId("test-user")
            .createdAt(Instant.now())
            .build();

    adapter.save(session);

    int threadCount = 10;
    CountDownLatch latch = new CountDownLatch(threadCount);
    int[] successCount = {0};

    for (int i = 0; i < threadCount; i++) {
      new Thread(
              () -> {
                Optional<AuthorizationSession> found = adapter.findByAuthorizationCode(code);
                if (found.isPresent()) {
                  synchronized (successCount) {
                    successCount[0]++;
                  }
                }
                latch.countDown();
              })
          .start();
    }

    latch.await(5, TimeUnit.SECONDS);

    assertThat(successCount[0]).isEqualTo(threadCount);
  }

  // ========== Edge Cases ==========

  @Test
  @DisplayName("Should handle special characters in authorization code")
  void shouldHandleSpecialCharactersInCode() {
    String code = "code-with-special_chars.123!@#";
    AuthorizationSession session =
        AuthorizationSession.builder()
            .authorizationCode(code)
            .userId("test-user")
            .createdAt(Instant.now())
            .build();

    adapter.save(session);

    Optional<AuthorizationSession> found = adapter.findByAuthorizationCode(code);
    assertThat(found).isPresent();
    assertThat(found.get().getAuthorizationCode()).isEqualTo(code);
  }

  @Test
  @DisplayName("Should handle very long authorization codes")
  void shouldHandleLongAuthorizationCodes() {
    String code =
        UUID.randomUUID().toString()
            + "-"
            + UUID.randomUUID().toString()
            + "-"
            + UUID.randomUUID().toString();
    AuthorizationSession session =
        AuthorizationSession.builder()
            .authorizationCode(code)
            .userId("test-user")
            .createdAt(Instant.now())
            .build();

    adapter.save(session);

    Optional<AuthorizationSession> found = adapter.findByAuthorizationCode(code);
    assertThat(found).isPresent();
  }

  @Test
  @DisplayName("Should handle multiple sessions from same user")
  void shouldHandleMultipleSessionsFromSameUser() {
    String userId = "test-user";

    for (int i = 0; i < 5; i++) {
      String code = UUID.randomUUID().toString();
      AuthorizationSession session =
          AuthorizationSession.builder()
              .authorizationCode(code)
              .userId(userId)
              .createdAt(Instant.now())
              .build();
      adapter.save(session);
    }

    var keys = redisTemplate.keys("wedge:test:*");
    assertThat(keys).isNotNull();
    assertThat(keys).hasSize(5);
  }
}
