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
   * Authenticates a user with username and password.
   *
   * @param username the username
   * @param password the password
   * @return Optional containing the authenticated User if successful, empty otherwise
   */
  public Optional<User> execute(String username, String password) {
    log.debug("Authenticating user: {}", username);

    if (username == null || username.isBlank()) {
      log.warn("Authentication failed: username is blank");
      return Optional.empty();
    }

    if (password == null || password.isBlank()) {
      log.warn("Authentication failed: password is blank");
      return Optional.empty();
    }

    Optional<User> user = userProvider.validateCredentials(username, password);

    if (user.isPresent()) {
      log.info("User authenticated successfully: {}", username);
    } else {
      log.warn("Authentication failed for user: {}", username);
    }

    return user;
  }
}
