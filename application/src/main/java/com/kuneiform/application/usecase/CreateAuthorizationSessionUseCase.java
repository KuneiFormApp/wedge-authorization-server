package com.kuneiform.application.usecase;

import com.kuneiform.domain.model.AuthorizationSession;
import com.kuneiform.domain.model.OAuthClient;
import com.kuneiform.domain.model.User;
import com.kuneiform.domain.port.ClientRepository;
import com.kuneiform.domain.port.SessionStorage;
import com.kuneiform.domain.port.UserProviderPort;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Use case for creating an OAuth authorization session. */
@Slf4j
@RequiredArgsConstructor
public class CreateAuthorizationSessionUseCase {

  private final ClientRepository clientRepository;
  private final SessionStorage sessionStorage;
  private final UserProviderPort userProviderPort;
  private final int sessionTtlSeconds;

  /**
   * Creates an authorization session for a user.
   *
   * @param user the authenticated user
   * @param clientId the OAuth client ID
   * @param redirectUri the redirect URI
   * @param scopes the requested scopes
   * @param state the OAuth state parameter
   * @param codeChallenge the PKCE code challenge
   * @param codeChallengeMethod the PKCE code challenge method
   * @return the authorization code if successful, empty otherwise
   */
  public Optional<String> execute(
      User user,
      String clientId,
      String redirectUri,
      Set<String> scopes,
      String state,
      String codeChallenge,
      String codeChallengeMethod) {

    log.debug(
        "Creating authorization session for user: {}, client: {}", user.getUserId(), clientId);

    Optional<OAuthClient> clientOpt = clientRepository.findByClientId(clientId);
    if (clientOpt.isEmpty()) {
      log.warn("Client not found: {}", clientId);
      return Optional.empty();
    }

    OAuthClient client = clientOpt.get();

    if (!client.isValidRedirectUri(redirectUri)) {
      log.warn("Invalid redirect URI for client {}: {}", clientId, redirectUri);
      return Optional.empty();
    }

    if (client.isRequirePkce() && (codeChallenge == null || codeChallenge.isBlank())) {
      log.warn("PKCE required but code_challenge not provided for client: {}", clientId);
      return Optional.empty();
    }

    Set<String> authorizedScopes =
        scopes.stream().filter(client::isAllowedScope).collect(Collectors.toSet());

    if (authorizedScopes.isEmpty()) {
      log.warn("No valid scopes requested for client: {}", clientId);
      return Optional.empty();
    }

    // Double-check strategy: Validate scopes with User Provider
    // This catches scenarios where the user is already authenticated but not authorized for these
    // scopes
    boolean scopesValid =
        userProviderPort.validateScopes(
            clientId, client.getTenantId(), user.getUserId(), authorizedScopes);
    if (!scopesValid) {
      log.warn(
          "User provider rejected scopes: {} for user: {} and client: {}",
          authorizedScopes,
          user.getUserId(),
          clientId);
      return Optional.empty();
    }

    String authorizationCode = generateAuthorizationCode();

    Instant now = Instant.now();
    AuthorizationSession session =
        AuthorizationSession.builder()
            .sessionId(UUID.randomUUID().toString())
            .authorizationCode(authorizationCode)
            .userId(user.getUserId())
            .clientId(clientId)
            .authorizedScopes(authorizedScopes)
            .redirectUri(redirectUri)
            .state(state)
            .codeChallenge(codeChallenge)
            .codeChallengeMethod(codeChallengeMethod)
            .createdAt(now)
            .expiresAt(now.plusSeconds(sessionTtlSeconds))
            .build();

    sessionStorage.save(session);

    log.info(
        "Authorization session created: code={}, user={}, client={}",
        authorizationCode,
        user.getUserId(),
        clientId);

    return Optional.of(authorizationCode);
  }

  private String generateAuthorizationCode() {
    return UUID.randomUUID().toString().replace("-", "");
  }
}
