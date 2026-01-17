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
  String state;

  String codeChallenge;
  String codeChallengeMethod;

  @With Instant createdAt;
  @With Instant expiresAt;

  public boolean isExpired() {
    return expiresAt != null && Instant.now().isAfter(expiresAt);
  }

  public boolean requiresPkce() {
    return codeChallenge != null && !codeChallenge.isBlank();
  }
}
