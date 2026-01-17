package com.kuneiform.infraestructure.adapter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.kuneiform.domain.model.AuthorizationSession;
import com.kuneiform.infraestructure.config.properties.WedgeConfigProperties;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class RedisSessionStorageAdapterTest {

  @Mock private RedisTemplate<String, AuthorizationSession> redisTemplate;
  @Mock private StringRedisTemplate stringRedisTemplate;
  @Mock private WedgeConfigProperties properties;
  @Mock private WedgeConfigProperties.SessionConfig sessionConfig;
  @Mock private WedgeConfigProperties.RedisConfig redisConfig;

  @Mock private ValueOperations<String, AuthorizationSession> valueOps;
  @Mock private SetOperations<String, String> setOps;

  private RedisSessionStorageAdapter adapter;

  @BeforeEach
  void setUp() {
    when(properties.getSession()).thenReturn(sessionConfig);
    when(sessionConfig.getRedis()).thenReturn(redisConfig);
    when(redisConfig.getNamespace()).thenReturn("wedge");
    when(sessionConfig.getAuthTtl()).thenReturn(600);

    // Mock Redis ops
    lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
    lenient().when(stringRedisTemplate.opsForSet()).thenReturn(setOps);

    adapter = new RedisSessionStorageAdapter(redisTemplate, stringRedisTemplate, properties);
    adapter.init();
  }

  @Test
  void shouldSaveSessionAndAddToUserIndex() {
    // Given
    AuthorizationSession session = createSession("code-123", "user-123");

    // When
    adapter.save(session);

    // Then
    verify(valueOps).set(eq("wedge:session:code-123"), eq(session), any());
    verify(setOps).add(eq("wedge:user:user-123"), eq("code-123"));
    verify(stringRedisTemplate).expire(eq("wedge:user:user-123"), any(java.time.Duration.class));
  }

  @Test
  void shouldFindSessionByAuthCode() {
    // Given
    AuthorizationSession session = createSession("code-123", "user-123");
    when(valueOps.get("wedge:session:code-123")).thenReturn(session);

    // When
    Optional<AuthorizationSession> result = adapter.findByAuthorizationCode("code-123");

    // Then
    assertTrue(result.isPresent());
    assertEquals("code-123", result.get().getAuthorizationCode());
  }

  @Test
  void shouldReturnEmptyForNonExistentCode() {
    // Given
    when(valueOps.get("wedge:session:unknown")).thenReturn(null);

    // When
    Optional<AuthorizationSession> result = adapter.findByAuthorizationCode("unknown");

    // Then
    assertTrue(result.isEmpty());
  }

  @Test
  void shouldReturnEmptyAndCleanupWhenSessionExpired() {
    // Given
    // Create an expired session
    AuthorizationSession expiredSession =
        AuthorizationSession.builder()
            .authorizationCode("code-expired")
            .userId("user-123")
            .expiresAt(Instant.now().minusSeconds(100))
            .build();

    when(valueOps.get("wedge:session:code-expired")).thenReturn(expiredSession);

    // When
    Optional<AuthorizationSession> result = adapter.findByAuthorizationCode("code-expired");

    // Then
    assertTrue(result.isEmpty());
    verify(redisTemplate).delete("wedge:session:code-expired");
    // Should also clean from user index
    verify(setOps).remove("wedge:user:user-123", "code-expired");
  }

  @Test
  void shouldFindSessionsByUserId() {
    // Given
    String userId = "user-123";
    Set<String> authCodes = Set.of("code-1", "code-2");
    when(setOps.members("wedge:user:user-123")).thenReturn(authCodes);

    AuthorizationSession session1 = createSession("code-1", userId);
    AuthorizationSession session2 = createSession("code-2", userId);

    when(valueOps.get("wedge:session:code-1")).thenReturn(session1);
    when(valueOps.get("wedge:session:code-2")).thenReturn(session2);

    // When
    var sessions = adapter.findByUserId(userId);

    // Then
    assertEquals(2, sessions.size());
    assertTrue(sessions.contains(session1));
    assertTrue(sessions.contains(session2));
  }

  @Test
  void shouldHandleRedisExceptionsGracefully() {
    // Given
    AuthorizationSession session = createSession("code-error", "user-123");
    doThrow(new RuntimeException("Redis down"))
        .when(valueOps)
        .set(any(), any(), any(java.time.Duration.class));

    // When & Then
    assertThrows(RuntimeException.class, () -> adapter.save(session));
  }

  @Test
  void shouldCleanupStaleIndexEntries() {
    // Given - User index has 2 codes, but one is missing/expired in Redis
    String userId = "user-stale";
    Set<String> authCodes = Set.of("code-active", "code-stale");
    when(setOps.members("wedge:user:user-stale")).thenReturn(authCodes);

    AuthorizationSession activeSession = createSession("code-active", userId);

    // code-active exists
    when(valueOps.get("wedge:session:code-active")).thenReturn(activeSession);
    // code-stale returns null (TTL expired)
    when(valueOps.get("wedge:session:code-stale")).thenReturn(null);

    // When
    var sessions = adapter.findByUserId(userId);

    // Then
    assertEquals(1, sessions.size());
    // Should remove stale code from index
    verify(setOps).remove(eq("wedge:user:user-stale"), any(Object[].class));
  }

  @Test
  void shouldDeleteSession() {
    // Given
    AuthorizationSession session = createSession("code-del", "user-del");
    when(valueOps.get("wedge:session:code-del")).thenReturn(session);

    // When
    adapter.deleteByAuthorizationCode("code-del");

    // Then
    verify(redisTemplate).delete("wedge:session:code-del");
    verify(setOps).remove("wedge:user:user-del", "code-del");
  }

  private AuthorizationSession createSession(String authorizationCode, String userId) {
    return AuthorizationSession.builder()
        .sessionId("sess-" + authorizationCode)
        .authorizationCode(authorizationCode)
        .userId(userId)
        .expiresAt(Instant.now().plusSeconds(600))
        .build();
  }
}
