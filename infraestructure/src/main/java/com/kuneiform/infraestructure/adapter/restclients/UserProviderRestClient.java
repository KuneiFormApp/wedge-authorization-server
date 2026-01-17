package com.kuneiform.infraestructure.adapter.restclients;

import com.kuneiform.infraestructure.adapter.models.UserResponse;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * REST client for communicating with the external user provider API.
 *
 * <p>Handles all HTTP calls to the user provider including authentication, user lookup, and MFA
 * registration.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserProviderRestClient {

  private final RestClient restClient;

  public Optional<UserResponse> findByUsername(String endpoint, String username) {
    try {
      UserResponse response =
          restClient
              .get()
              .uri(endpoint + "/find?username={username}", username)
              .retrieve()
              .onStatus(
                  status -> status.value() == HttpStatus.NOT_FOUND.value(),
                  (request, responseEntity) -> {
                    log.debug("User not found: {}", username);
                  })
              .body(UserResponse.class);

      return Optional.ofNullable(response);
    } catch (Exception e) {
      log.error("Error finding user: {}", username, e);
      return Optional.empty();
    }
  }

  public Optional<UserResponse> validateCredentials(
      String endpoint, String username, String password) {
    try {
      UserResponse response =
          restClient
              .post()
              .uri(endpoint)
              .body(Map.of("username", username, "password", password))
              .retrieve()
              .onStatus(
                  status -> status.value() == HttpStatus.UNAUTHORIZED.value(),
                  (request, responseEntity) -> {
                    log.debug("Invalid credentials for user: {}", username);
                  })
              .body(UserResponse.class);

      return Optional.ofNullable(response);
    } catch (Exception e) {
      log.error("Error validating credentials for user: {}", username, e);
      return Optional.empty();
    }
  }

  /**
   * @param mfaEndpoint the MFA registration endpoint (with {userId} already replaced)
   */
  public boolean registerMfa(String mfaEndpoint, String mfaSecret, String mfaKeyId) {
    try {
      Map<String, Object> requestBody =
          Map.of(
              "mfaSecret", mfaSecret,
              "twoFaRegistered", true,
              "mfaKeyId", mfaKeyId);

      restClient
          .patch()
          .uri(mfaEndpoint)
          .body(requestBody)
          .retrieve()
          .onStatus(
              status -> status.value() != HttpStatus.NO_CONTENT.value(),
              (request, response) -> {
                String errorMsg =
                    String.format(
                        "Failed to register MFA with user provider: HTTP %s",
                        response.getStatusCode());
                log.error(errorMsg);
                throw new RuntimeException(errorMsg);
              })
          .toBodilessEntity();

      log.info("Successfully registered MFA with user provider");
      return true;

    } catch (Exception e) {
      log.error("Error registering MFA with user provider", e);
      return false;
    }
  }
}
