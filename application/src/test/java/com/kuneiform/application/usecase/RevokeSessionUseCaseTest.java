package com.kuneiform.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.kuneiform.domain.model.AuthorizationSession;
import com.kuneiform.domain.port.SessionStorage;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RevokeSessionUseCaseTest {

  @Mock private SessionStorage sessionStorage;

  private RevokeSessionUseCase useCase;

  @BeforeEach
  void setUp() {
    useCase = new RevokeSessionUseCase(sessionStorage);
  }

  @Test
  void shouldRevokeSessionWhenSessionExistsAndBelongsToUser() {
    // Given
    String sessionId = "session-123";
    String userId = "user-456";

    AuthorizationSession session = createSession(sessionId, userId);
    when(sessionStorage.findByAuthorizationCode(sessionId)).thenReturn(Optional.of(session));

    // When
    useCase.execute(sessionId, userId);

    // Then
    verify(sessionStorage).findByAuthorizationCode(sessionId);
    verify(sessionStorage).deleteByAuthorizationCode(sessionId);
  }

  @Test
  void shouldThrowExceptionWhenSessionNotFound() {
    // Given
    String sessionId = "nonexistent-session";
    String userId = "user-456";

    when(sessionStorage.findByAuthorizationCode(sessionId)).thenReturn(Optional.empty());

    // When & Then
    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(sessionId, userId));

    assertEquals("Session not found", exception.getMessage());
    verify(sessionStorage).findByAuthorizationCode(sessionId);
    verify(sessionStorage, never()).deleteByAuthorizationCode(any());
  }

  @Test
  void shouldThrowExceptionWhenSessionBelongsToAnotherUser() {
    // Given
    String sessionId = "session-123";
    String requestingUserId = "user-456";
    String actualOwnerId = "user-789";

    AuthorizationSession session = createSession(sessionId, actualOwnerId);
    when(sessionStorage.findByAuthorizationCode(sessionId)).thenReturn(Optional.of(session));

    // When & Then
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> useCase.execute(sessionId, requestingUserId));

    assertEquals("Session does not belong to user", exception.getMessage());
    verify(sessionStorage).findByAuthorizationCode(sessionId);
    verify(sessionStorage, never()).deleteByAuthorizationCode(any());
  }

  private AuthorizationSession createSession(String authorizationCode, String userId) {
    return AuthorizationSession.builder()
        .sessionId("sid-" + authorizationCode)
        .authorizationCode(authorizationCode)
        .userId(userId)
        .clientId("test-client")
        .authorizedScopes(Set.of("openid"))
        .redirectUri("http://localhost:3000/callback")
        .createdAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(600))
        .build();
  }
}
