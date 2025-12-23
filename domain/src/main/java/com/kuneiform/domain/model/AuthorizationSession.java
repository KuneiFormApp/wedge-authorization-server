package com.kuneiform.domain.model;

import java.beans.ConstructorProperties;
import java.time.Instant;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.With;

@Value
@Builder
@AllArgsConstructor(
    onConstructor_ =
        @ConstructorProperties({
          "sessionId",
          "authorizationCode",
          "userId",
          "clientId",
          "authorizedScopes",
          "redirectUri",
          "state",
          "codeChallenge",
          "codeChallengeMethod",
          "createdAt",
          "expiresAt"
        }))
public class AuthorizationSession {
  String sessionId;
  String authorizationCode;

  String userId;
  String clientId;

  Set<String> authorizedScopes;
  String redirectUri;
  String state; // OAuth state parameter for CSRF protection

  // PKCE parameters
  String codeChallenge;
  String codeChallengeMethod; // "S256" or "plain"

  @With Instant createdAt;
  @With Instant expiresAt;

  // Checks if this session has expired.
  public boolean isExpired() {
    return expiresAt != null && Instant.now().isAfter(expiresAt);
  }

  // Checks if the session requires PKCE validation.
  public boolean requiresPkce() {
    return codeChallenge != null && !codeChallenge.isBlank();
  }
}
