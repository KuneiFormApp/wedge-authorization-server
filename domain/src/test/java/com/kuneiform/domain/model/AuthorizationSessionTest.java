package com.kuneiform.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AuthorizationSessionTest {

  @Test
  void shouldBuildAuthorizationSession() {
    Instant now = Instant.now();
    AuthorizationSession session =
        AuthorizationSession.builder()
            .sessionId("session-123")
            .authorizationCode("code-123")
            .userId("user-123")
            .clientId("client-123")
            .authorizedScopes(Set.of("openid", "profile"))
            .redirectUri("http://localhost:3000/callback")
            .state("state-123")
            .codeChallenge("challenge")
            .codeChallengeMethod("S256")
            .createdAt(now)
            .expiresAt(now.plusSeconds(600))
            .build();

    assertEquals("session-123", session.getSessionId());
    assertEquals("code-123", session.getAuthorizationCode());
    assertEquals("user-123", session.getUserId());
    assertEquals("client-123", session.getClientId());
    assertEquals(Set.of("openid", "profile"), session.getAuthorizedScopes());
    assertEquals("http://localhost:3000/callback", session.getRedirectUri());
    assertEquals("state-123", session.getState());
    assertEquals("challenge", session.getCodeChallenge());
    assertEquals("S256", session.getCodeChallengeMethod());
  }

  @Test
  void shouldDetectExpiredSession() {
    Instant pastTime = Instant.now().minusSeconds(600);
    AuthorizationSession session =
        AuthorizationSession.builder()
            .sessionId("session-123")
            .authorizationCode("code-123")
            .userId("user-123")
            .clientId("client-123")
            .authorizedScopes(Set.of("openid"))
            .redirectUri("http://localhost:3000/callback")
            .expiresAt(pastTime)
            .build();

    assertTrue(session.isExpired());
  }

  @Test
  void shouldDetectNonExpiredSession() {
    Instant futureTime = Instant.now().plusSeconds(600);
    AuthorizationSession session =
        AuthorizationSession.builder()
            .sessionId("session-123")
            .authorizationCode("code-123")
            .userId("user-123")
            .clientId("client-123")
            .authorizedScopes(Set.of("openid"))
            .redirectUri("http://localhost:3000/callback")
            .expiresAt(futureTime)
            .build();

    assertFalse(session.isExpired());
  }

  @Test
  void shouldDetectPkceRequired() {
    AuthorizationSession sessionWithPkce =
        AuthorizationSession.builder()
            .sessionId("session-123")
            .authorizationCode("code-123")
            .userId("user-123")
            .clientId("client-123")
            .authorizedScopes(Set.of("openid"))
            .redirectUri("http://localhost:3000/callback")
            .codeChallenge("challenge")
            .codeChallengeMethod("S256")
            .expiresAt(Instant.now().plusSeconds(600))
            .build();

    assertTrue(sessionWithPkce.requiresPkce());
  }

  @Test
  void shouldDetectPkceNotRequired() {
    AuthorizationSession sessionWithoutPkce =
        AuthorizationSession.builder()
            .sessionId("session-123")
            .authorizationCode("code-123")
            .userId("user-123")
            .clientId("client-123")
            .authorizedScopes(Set.of("openid"))
            .redirectUri("http://localhost:3000/callback")
            .expiresAt(Instant.now().plusSeconds(600))
            .build();

    assertFalse(sessionWithoutPkce.requiresPkce());
  }
}
