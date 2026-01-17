package com.kuneiform.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.kuneiform.domain.model.AuthorizationSession;
import com.kuneiform.domain.model.OAuthClient;
import com.kuneiform.domain.model.User;
import com.kuneiform.domain.port.ClientRepository;
import com.kuneiform.domain.port.SessionStorage;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateAuthorizationSessionUseCaseTest {

  @Mock private ClientRepository clientRepository;
  @Mock private SessionStorage sessionStorage;

  private CreateAuthorizationSessionUseCase useCase;
  private User testUser;
  private OAuthClient testClient;

  @BeforeEach
  void setUp() {
    useCase = new CreateAuthorizationSessionUseCase(clientRepository, sessionStorage, 600);

    testUser = User.builder().userId("user-123").username("testuser").build();

    testClient =
        OAuthClient.builder()
            .clientId("test-client")
            .redirectUris(Set.of("http://localhost:3000/callback"))
            .scopes(Set.of("openid", "profile", "email"))
            .requirePkce(true)
            .build();
  }

  @Test
  void shouldCreateAuthorizationSession() {
    when(clientRepository.findByClientId("test-client")).thenReturn(Optional.of(testClient));

    Optional<String> result =
        useCase.execute(
            testUser,
            "test-client",
            "http://localhost:3000/callback",
            Set.of("openid", "profile"),
            "state-123",
            "challenge",
            "S256");

    assertTrue(result.isPresent());
    assertFalse(result.get().isEmpty());

    ArgumentCaptor<AuthorizationSession> captor =
        ArgumentCaptor.forClass(AuthorizationSession.class);
    verify(sessionStorage).save(captor.capture());

    AuthorizationSession savedSession = captor.getValue();
    assertEquals("user-123", savedSession.getUserId());
    assertEquals("test-client", savedSession.getClientId());
    assertEquals("http://localhost:3000/callback", savedSession.getRedirectUri());
    assertEquals("state-123", savedSession.getState());
    assertEquals("challenge", savedSession.getCodeChallenge());
    assertEquals("S256", savedSession.getCodeChallengeMethod());
    assertEquals(Set.of("openid", "profile"), savedSession.getAuthorizedScopes());
  }

  @Test
  void shouldRejectNonExistentClient() {
    when(clientRepository.findByClientId("unknown-client")).thenReturn(Optional.empty());

    Optional<String> result =
        useCase.execute(
            testUser,
            "unknown-client",
            "http://localhost:3000/callback",
            Set.of("openid"),
            "state-123",
            "challenge",
            "S256");

    assertTrue(result.isEmpty());
    verifyNoInteractions(sessionStorage);
  }

  @Test
  void shouldRejectInvalidRedirectUri() {
    when(clientRepository.findByClientId("test-client")).thenReturn(Optional.of(testClient));

    Optional<String> result =
        useCase.execute(
            testUser,
            "test-client",
            "http://malicious.com/callback",
            Set.of("openid"),
            "state-123",
            "challenge",
            "S256");

    assertTrue(result.isEmpty());
    verifyNoInteractions(sessionStorage);
  }

  @Test
  void shouldRejectMissingPkceForPublicClient() {
    when(clientRepository.findByClientId("test-client")).thenReturn(Optional.of(testClient));

    Optional<String> result =
        useCase.execute(
            testUser,
            "test-client",
            "http://localhost:3000/callback",
            Set.of("openid"),
            "state-123",
            null,
            null);

    assertTrue(result.isEmpty());
    verifyNoInteractions(sessionStorage);
  }

  @Test
  void shouldFilterInvalidScopes() {
    when(clientRepository.findByClientId("test-client")).thenReturn(Optional.of(testClient));

    Optional<String> result =
        useCase.execute(
            testUser,
            "test-client",
            "http://localhost:3000/callback",
            Set.of("openid", "admin"), // 'admin' is not allowed
            "state-123",
            "challenge",
            "S256");

    assertTrue(result.isPresent());

    ArgumentCaptor<AuthorizationSession> captor =
        ArgumentCaptor.forClass(AuthorizationSession.class);
    verify(sessionStorage).save(captor.capture());

    AuthorizationSession savedSession = captor.getValue();
    assertEquals(Set.of("openid"), savedSession.getAuthorizedScopes()); // 'admin' filtered out
  }

  @Test
  void shouldRejectWhenAllScopesInvalid() {
    when(clientRepository.findByClientId("test-client")).thenReturn(Optional.of(testClient));

    Optional<String> result =
        useCase.execute(
            testUser,
            "test-client",
            "http://localhost:3000/callback",
            Set.of("admin", "superadmin"), // Both invalid
            "state-123",
            "challenge",
            "S256");

    assertTrue(result.isEmpty());
    verifyNoInteractions(sessionStorage);
  }
}
