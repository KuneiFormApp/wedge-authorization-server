package com.kuneiform.application.usecase;

import com.kuneiform.domain.model.User;
import com.kuneiform.domain.port.UserProviderPort;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Use case for authenticating users via the HTTP-based user provider. */
@Slf4j
@RequiredArgsConstructor
public class AuthenticateUserUseCase {

  private final UserProviderPort userProviderPort;

  /**
   * Authenticates a user with username and password for a specific client.
   *
   * @param clientId the client ID requesting authentication
   * @param tenantId the optional tenant ID (if known explicitly)
   * @param username the username
   * @param password the password
   * @return Optional containing the authenticated User if successful, empty otherwise
   */
  public Optional<User> execute(
      String clientId, String tenantId, String username, String password, Set<String> scopes) {
    log.debug("Authenticating user: {} for client: {} (tenant: {})", username, clientId, tenantId);

    if (clientId == null && tenantId == null) {
      log.warn("Authentication failed: both clientId and tenantId are blank or null");
      return Optional.empty();
    }

    if ((clientId == null || clientId.isBlank()) && (tenantId == null || tenantId.isBlank())) {
      log.warn("Authentication failed: both clientId and tenantId are blank");
      return Optional.empty();
    }

    if (username == null || username.isBlank()) {
      log.warn("Authentication failed: username is blank");
      return Optional.empty();
    }

    if (password == null || password.isBlank()) {
      log.warn("Authentication failed: password is blank");
      return Optional.empty();
    }

    Optional<User> user =
        userProviderPort.validateCredentials(clientId, tenantId, username, password, scopes);

    if (user.isPresent()) {
      log.info("User authenticated successfully: {} for client: {}", username, clientId);
    } else {
      log.warn("Authentication failed for user: {} for client: {}", username, clientId);
    }

    return user;
  }
}
