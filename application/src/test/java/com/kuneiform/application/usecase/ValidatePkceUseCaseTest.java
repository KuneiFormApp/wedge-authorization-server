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
class ValidatePkceUseCaseTest {

  @Mock private SessionStorage sessionStorage;

  private ValidatePkceUseCase useCase;

  @BeforeEach
  void setUp() {
    useCase = new ValidatePkceUseCase(sessionStorage);
  }

  @Test
  void shouldValidateS256Challenge() {
    String verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
    String challenge = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM";

    AuthorizationSession session =
        AuthorizationSession.builder()
            .authorizationCode("code-123")
            .userId("user-123")
            .clientId("client-123")
            .authorizedScopes(Set.of("openid"))
            .redirectUri("http://localhost:3000/callback")
            .codeChallenge(challenge)
            .codeChallengeMethod("S256")
            .createdAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(600))
            .build();

    when(sessionStorage.findByAuthorizationCode("code-123")).thenReturn(Optional.of(session));

    boolean result = useCase.execute("code-123", verifier);

    assertTrue(result);
  }

  @Test
  void shouldValidatePlainChallenge() {
    String verifier = "plain-verifier";
    String challenge = "plain-verifier";

    AuthorizationSession session =
        AuthorizationSession.builder()
            .authorizationCode("code-123")
            .userId("user-123")
            .clientId("client-123")
            .authorizedScopes(Set.of("openid"))
            .redirectUri("http://localhost:3000/callback")
            .codeChallenge(challenge)
            .codeChallengeMethod("plain")
            .createdAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(600))
            .build();

    when(sessionStorage.findByAuthorizationCode("code-123")).thenReturn(Optional.of(session));

    boolean result = useCase.execute("code-123", verifier);

    assertTrue(result);
  }

  @Test
  void shouldRejectInvalidVerifier() {
    String validVerifier = "valid-verifier";
    String invalidVerifier = "invalid-verifier";
    String challenge = "valid-verifier";

    AuthorizationSession session =
        AuthorizationSession.builder()
            .authorizationCode("code-123")
            .userId("user-123")
            .clientId("client-123")
            .authorizedScopes(Set.of("openid"))
            .redirectUri("http://localhost:3000/callback")
            .codeChallenge(challenge)
            .codeChallengeMethod("plain")
            .createdAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(600))
            .build();

    when(sessionStorage.findByAuthorizationCode("code-123")).thenReturn(Optional.of(session));

    boolean result = useCase.execute("code-123", invalidVerifier);

    assertFalse(result);
  }

  @Test
  void shouldPassWhenPkceNotRequired() {
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

    when(sessionStorage.findByAuthorizationCode("code-123")).thenReturn(Optional.of(session));

    boolean result = useCase.execute("code-123", null);

    assertTrue(result);
  }

  @Test
  void shouldRejectMissingVerifierWhenPkceRequired() {
    AuthorizationSession session =
        AuthorizationSession.builder()
            .authorizationCode("code-123")
            .userId("user-123")
            .clientId("client-123")
            .authorizedScopes(Set.of("openid"))
            .redirectUri("http://localhost:3000/callback")
            .codeChallenge("challenge")
            .codeChallengeMethod("S256")
            .createdAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(600))
            .build();

    when(sessionStorage.findByAuthorizationCode("code-123")).thenReturn(Optional.of(session));

    boolean result = useCase.execute("code-123", null);

    assertFalse(result);
  }

  @Test
  void shouldRejectNonExistentSession() {
    when(sessionStorage.findByAuthorizationCode("invalid-code")).thenReturn(Optional.empty());

    boolean result = useCase.execute("invalid-code", "verifier");

    assertFalse(result);
  }

  @Test
  void shouldRejectUnsupportedChallengeMethod() {
    AuthorizationSession session =
        AuthorizationSession.builder()
            .authorizationCode("code-123")
            .userId("user-123")
            .clientId("client-123")
            .authorizedScopes(Set.of("openid"))
            .redirectUri("http://localhost:3000/callback")
            .codeChallenge("challenge")
            .codeChallengeMethod("unknown")
            .createdAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(600))
            .build();

    when(sessionStorage.findByAuthorizationCode("code-123")).thenReturn(Optional.of(session));

    boolean result = useCase.execute("code-123", "verifier");

    assertFalse(result);
  }
}
