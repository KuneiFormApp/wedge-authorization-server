package com.kuneiform.infraestructure.adapter;

import com.kuneiform.domain.model.User;
import com.kuneiform.domain.port.UserProvider;
import com.kuneiform.infraestructure.config.properties.WedgeConfigProperties;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** HTTP-based implementation of UserProvider. Calls an external HTTP endpoint to validate users. */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "wedge.user-provider.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class HttpUserProviderAdapter implements UserProvider {

  private final RestClient restClient;
  private final WedgeConfigProperties config;

  @Override
  public Optional<User> findByUsername(String username) {
    log.debug("Finding user by username: {}", username);

    try {
      UserResponse response =
          restClient
              .get()
              .uri(config.getUserProvider().getEndpoint() + "/find?username={username}", username)
              .retrieve()
              .onStatus(
                  status -> status.value() == HttpStatus.NOT_FOUND.value(),
                  (request, responseEntity) -> {
                    log.debug("User not found: {}", username);
                  })
              .body(UserResponse.class);

      if (response != null) {
        return Optional.of(mapToUser(response));
      }
    } catch (Exception e) {
      log.error("Error finding user: {}", username, e);
    }

    return Optional.empty();
  }

  @Override
  public Optional<User> validateCredentials(String username, String password) {
    log.debug("Validating credentials for user: {}", username);

    try {
      UserResponse response =
          restClient
              .post()
              .uri(config.getUserProvider().getEndpoint())
              .body(Map.of("username", username, "password", password))
              .retrieve()
              .onStatus(
                  status -> status.value() == HttpStatus.UNAUTHORIZED.value(),
                  (request, responseEntity) -> {
                    log.debug("Invalid credentials for user: {}", username);
                  })
              .body(UserResponse.class);

      if (response != null) {
        return Optional.of(mapToUser(response));
      }
    } catch (Exception e) {
      log.error("Error validating credentials for user: {}", username, e);
    }

    return Optional.empty();
  }

  private User mapToUser(UserResponse response) {
    return User.builder()
        .userId(response.userId())
        .username(response.username())
        .email(response.email())
        .metadata(response.metadata())
        .build();
  }

  /** Expected response from the user provider HTTP endpoint. */
  private record UserResponse(
      String userId, String username, String email, Map<String, Object> metadata) {}
}
