package com.kuneiform.application.usecase;

import com.kuneiform.domain.model.User;
import com.kuneiform.domain.port.UserProvider;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Use case for authenticating users via the HTTP-based user provider. */
@Slf4j
@RequiredArgsConstructor
public class AuthenticateUserUseCase {

  private final UserProvider userProvider;

  /**
   * Authenticates a user with username and password for a specific client.
   *
   * @param clientId the client ID requesting authentication
   * @param username the username
   * @param password the password
   * @return Optional containing the authenticated User if successful, empty otherwise
   */
  public Optional<User> execute(String clientId, String username, String password) {
    log.debug("Authenticating user: {} for client: {}", username, clientId);

    if (clientId == null || clientId.isBlank()) {
      log.warn("Authentication failed: clientId is blank");
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

    Optional<User> user = userProvider.validateCredentials(clientId, username, password);

    if (user.isPresent()) {
      log.info("User authenticated successfully: {} for client: {}", username, clientId);
    } else {
      log.warn("Authentication failed for user: {} for client: {}", username, clientId);
    }

    return user;
  }
}
