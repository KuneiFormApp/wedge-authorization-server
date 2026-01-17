package com.kuneiform.infraestructure.adapter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.kuneiform.domain.model.AuthorizationSession;
import com.kuneiform.infraestructure.config.properties.WedgeConfigProperties;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InMemorySessionStorageAdapterTest {

  private InMemorySessionStorageAdapter adapter;
  private WedgeConfigProperties properties;

  @BeforeEach
  void setUp() {
    // Mock WedgeConfigProperties with TTL = 600 seconds
    properties = mock(WedgeConfigProperties.class);
    WedgeConfigProperties.SessionConfig sessionConfig = new WedgeConfigProperties.SessionConfig();
    sessionConfig.setAuthTtl(600); // 10 minutes TTL

    when(properties.getSession()).thenReturn(sessionConfig);

    adapter = new InMemorySessionStorageAdapter(properties);
    adapter.init(); // Call @PostConstruct manually for test
  }

  @Test
  void shouldSaveAndRetrieveSession() {
    AuthorizationSession session =
        AuthorizationSession.builder()
            .sessionId("session-123")
            .authorizationCode("code-123")
            .userId("user-123")
            .clientId("client-123")
            .authorizedScopes(Set.of("openid"))
            .redirectUri("http://localhost:3000/callback")
            .createdAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(600))
            .build();

    adapter.save(session);

    Optional<AuthorizationSession> retrieved = adapter.findByAuthorizationCode("code-123");

    assertTrue(retrieved.isPresent());
    assertEquals("session-123", retrieved.get().getSessionId());
    assertEquals("user-123", retrieved.get().getUserId());
  }

  @Test
  void shouldReturnEmptyForNonExistentCode() {
    Optional<AuthorizationSession> result = adapter.findByAuthorizationCode("nonexistent");

    assertTrue(result.isEmpty());
  }

  @Test
  void shouldDeleteSession() {
    AuthorizationSession session =
        AuthorizationSession.builder()
            .authorizationCode("code-123")
            .userId("user-123")
            .clientId("client-123")
            .authorizedScopes(Set.of("openid"))
            .redirectUri("http://localhost:3000/callback")
            .createdAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(600))
            .build();

    adapter.save(session);
    adapter.deleteByAuthorizationCode("code-123");

    Optional<AuthorizationSession> result = adapter.findByAuthorizationCode("code-123");

    assertTrue(result.isEmpty());
  }

  @Test
  void shouldAutomaticallyRemoveExpiredSessionOnRetrieval() {
    // Create session that is already expired (domain expiration)
    AuthorizationSession expiredSession =
        AuthorizationSession.builder()
            .authorizationCode("code-123")
            .userId("user-123")
            .clientId("client-123")
            .authorizedScopes(Set.of("openid"))
            .redirectUri("http://localhost:3000/callback")
            .createdAt(Instant.now().minusSeconds(1000))
            .expiresAt(Instant.now().minusSeconds(100)) // Expired 100 seconds ago
            .build();

    adapter.save(expiredSession);

    // Should return empty because domain expiration check
    Optional<AuthorizationSession> result = adapter.findByAuthorizationCode("code-123");

    assertTrue(result.isEmpty());
  }

  @Test
  void shouldRespectTtlFromConfiguration() throws InterruptedException {
    // Set very short TTL for testing (1 second)
    WedgeConfigProperties.SessionConfig shortTtlConfig = new WedgeConfigProperties.SessionConfig();
    shortTtlConfig.setAuthTtl(1); // 1 second TTL

    when(properties.getSession()).thenReturn(shortTtlConfig);

    InMemorySessionStorageAdapter shortTtlAdapter = new InMemorySessionStorageAdapter(properties);
    shortTtlAdapter.init(); // Initialize Caffeine cache

    AuthorizationSession session =
        AuthorizationSession.builder()
            .authorizationCode("code-ttl")
            .userId("user-123")
            .clientId("client-123")
            .authorizedScopes(Set.of("openid"))
            .redirectUri("http://localhost:3000/callback")
            .createdAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600)) // Domain expiration far in future
            .build();

    shortTtlAdapter.save(session);

    // Should exist immediately
    assertTrue(shortTtlAdapter.findByAuthorizationCode("code-ttl").isPresent());

    // Wait for TTL to expire (1 second + buffer)
    Thread.sleep(1100);

    // Should be expired due to TTL
    Optional<AuthorizationSession> result = shortTtlAdapter.findByAuthorizationCode("code-ttl");
    assertTrue(result.isEmpty(), "Session should be expired after TTL");
  }

  @Test
  void shouldDeleteExpiredSessions() {
    AuthorizationSession validSession =
        AuthorizationSession.builder()
            .authorizationCode("code-valid")
            .userId("user-123")
            .clientId("client-123")
            .authorizedScopes(Set.of("openid"))
            .redirectUri("http://localhost:3000/callback")
            .createdAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(600))
            .build();

    AuthorizationSession expiredSession =
        AuthorizationSession.builder()
            .authorizationCode("code-expired")
            .userId("user-456")
            .clientId("client-456")
            .authorizedScopes(Set.of("openid"))
            .redirectUri("http://localhost:3000/callback")
            .createdAt(Instant.now().minusSeconds(1000))
            .expiresAt(Instant.now().minusSeconds(100)) // Domain expired
            .build();

    adapter.save(validSession);
    adapter.save(expiredSession);

    adapter.deleteExpiredSessions();

    assertTrue(adapter.findByAuthorizationCode("code-valid").isPresent());
    assertTrue(adapter.findByAuthorizationCode("code-expired").isEmpty());
  }

  @Test
  void shouldDeleteSessionsExpiredByTtl() throws InterruptedException {
    // Use short TTL for testing
    WedgeConfigProperties.SessionConfig shortTtlConfig = new WedgeConfigProperties.SessionConfig();
    shortTtlConfig.setAuthTtl(1); // 1 second TTL

    when(properties.getSession()).thenReturn(shortTtlConfig);

    InMemorySessionStorageAdapter shortTtlAdapter = new InMemorySessionStorageAdapter(properties);
    shortTtlAdapter.init(); // Initialize Caffeine cache

    AuthorizationSession session =
        AuthorizationSession.builder()
            .authorizationCode("code-cleanup")
            .userId("user-123")
            .clientId("client-123")
            .authorizedScopes(Set.of("openid"))
            .redirectUri("http://localhost:3000/callback")
            .createdAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600)) // Domain expiration far in future
            .build();

    shortTtlAdapter.save(session);

    // Wait for TTL expiration
    Thread.sleep(1100);

    // Run cleanup
    shortTtlAdapter.deleteExpiredSessions();

    // Session should be cleaned up
    assertTrue(shortTtlAdapter.findByAuthorizationCode("code-cleanup").isEmpty());
  }
}
