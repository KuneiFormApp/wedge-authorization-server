package com.kuneiform.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.kuneiform.domain.model.AuthorizationSession;
import com.kuneiform.domain.port.SessionStorage;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListUserSessionsUseCaseTest {

  @Mock private SessionStorage sessionStorage;

  private ListUserSessionsUseCase useCase;

  @BeforeEach
  void setUp() {
    useCase = new ListUserSessionsUseCase(sessionStorage);
  }

  @Test
  void shouldReturnSessionsWhenUserHasActiveSessions() {
    // Given
    String userId = "user-123";

    AuthorizationSession session1 =
        createSession("session-1", userId, "client-1", Instant.now().plusSeconds(600));
    AuthorizationSession session2 =
        createSession("session-2", userId, "client-2", Instant.now().plusSeconds(300));

    when(sessionStorage.findByUserId(userId)).thenReturn(List.of(session1, session2));

    // When
    List<AuthorizationSession> result = useCase.execute(userId);

    // Then
    assertNotNull(result);
    assertEquals(2, result.size());
    assertTrue(result.contains(session1));
    assertTrue(result.contains(session2));
    verify(sessionStorage).findByUserId(userId);
  }

  @Test
  void shouldReturnEmptyListWhenUserHasNoSessions() {
    // Given
    String userId = "user-no-sessions";

    when(sessionStorage.findByUserId(userId)).thenReturn(Collections.emptyList());

    // When
    List<AuthorizationSession> result = useCase.execute(userId);

    // Then
    assertNotNull(result);
    assertTrue(result.isEmpty());
    verify(sessionStorage).findByUserId(userId);
  }

  private AuthorizationSession createSession(
      String sessionId, String userId, String clientId, Instant expiresAt) {
    return AuthorizationSession.builder()
        .sessionId(sessionId)
        .authorizationCode("code-" + sessionId)
        .userId(userId)
        .clientId(clientId)
        .authorizedScopes(Set.of("openid"))
        .redirectUri("http://localhost:3000/callback")
        .createdAt(Instant.now())
        .expiresAt(expiresAt)
        .build();
  }
}
