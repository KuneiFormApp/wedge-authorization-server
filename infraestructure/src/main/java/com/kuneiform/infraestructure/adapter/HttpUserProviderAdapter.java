package com.kuneiform.infraestructure.adapter;

import com.kuneiform.domain.model.OAuthClient;
import com.kuneiform.domain.model.User;
import com.kuneiform.domain.model.UserProviderConfig;
import com.kuneiform.domain.port.ClientRepository;
import com.kuneiform.domain.port.UserProvider;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** HTTP-based implementation of UserProvider. Calls an external HTTP endpoint to validate users. */
@Slf4j
@Component
@RequiredArgsConstructor
public class HttpUserProviderAdapter implements UserProvider {

  private final RestClient restClient;
  private final ClientRepository clientRepository;

  @Override
  public Optional<User> findByUsername(String clientId, String username) {
    log.debug("Finding user by username: {} for client: {}", username, clientId);

    Optional<UserProviderConfig> userProviderConfig = getUserProviderConfig(clientId);
    if (userProviderConfig.isEmpty()) {
      return Optional.empty();
    }

    UserProviderConfig config = userProviderConfig.get();

    try {
      UserResponse response =
          restClient
              .get()
              .uri(config.getEndpoint() + "/find?username={username}", username)
              .retrieve()
              .onStatus(
                  status -> status.value() == HttpStatus.NOT_FOUND.value(),
                  (request, responseEntity) -> {
                    log.debug("User not found: {} for client: {}", username, clientId);
                  })
              .body(UserResponse.class);

      if (response != null) {
        return Optional.of(mapToUser(response));
      }
    } catch (Exception e) {
      log.error("Error finding user: {} for client: {}", username, clientId, e);
    }

    return Optional.empty();
  }

  @Override
  public Optional<User> validateCredentials(String clientId, String username, String password) {
    log.debug("Validating credentials for user: {} for client: {}", username, clientId);

    Optional<UserProviderConfig> userProviderConfig = getUserProviderConfig(clientId);
    if (userProviderConfig.isEmpty()) {
      return Optional.empty();
    }

    UserProviderConfig config = userProviderConfig.get();

    try {
      UserResponse response =
          restClient
              .post()
              .uri(config.getEndpoint())
              .body(Map.of("username", username, "password", password))
              .retrieve()
              .onStatus(
                  status -> status.value() == HttpStatus.UNAUTHORIZED.value(),
                  (request, responseEntity) -> {
                    log.debug(
                        "Invalid credentials for user: {} for client: {}", username, clientId);
                  })
              .body(UserResponse.class);

      if (response != null) {
        return Optional.of(mapToUser(response));
      }
    } catch (Exception e) {
      log.error("Error validating credentials for user: {} for client: {}", username, clientId, e);
    }

    return Optional.empty();
  }

  private Optional<UserProviderConfig> getUserProviderConfig(String clientId) {
    Optional<OAuthClient> clientOpt = clientRepository.findByClientId(clientId);

    if (clientOpt.isEmpty()) {
      log.error("Client not found for clientId: {}", clientId);
      return Optional.empty();
    }

    OAuthClient client = clientOpt.get();
    UserProviderConfig config = client.getUserProviderConfig();

    if (config == null || !config.isEnabled()) {
      log.warn("User provider is disabled or not configured for client: {}", clientId);
      return Optional.empty();
    }

    return Optional.of(config);
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
