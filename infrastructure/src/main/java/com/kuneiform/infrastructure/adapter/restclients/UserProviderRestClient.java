package com.kuneiform.infrastructure.adapter.restclients;

import com.kuneiform.domain.exception.UserProviderClientException;
import com.kuneiform.domain.exception.UserProviderException;
import com.kuneiform.infrastructure.adapter.models.UserProviderErrorResponse;
import com.kuneiform.infrastructure.adapter.models.UserResponse;
import com.kuneiform.infrastructure.config.UserProviderApiKeyProperties;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

/**
 * REST client for communicating with the external user provider API.
 *
 * <p>Uses circuit breaker pattern to prevent cascading failures when the user provider service
 * becomes unavailable, as authentication failures should not bring down the entire authorization
 * server.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserProviderRestClient {

  private final RestClient restClient;
  private final ObjectMapper objectMapper;
  private final UserProviderApiKeyProperties apiKeyProperties;

  @CircuitBreaker(name = "userProvider", fallbackMethod = "findByUsernameFallback")
  public Optional<UserResponse> findByUsername(String endpoint, String username)
      throws UserProviderException {
    try {

      var requestBuilder = restClient.get().uri(endpoint + "/find?username={username}", username);

      requestBuilder = setUpHeaders(requestBuilder);

      ResponseEntity<UserResponse> response =
          requestBuilder.retrieve().toEntity(UserResponse.class);

      return Optional.ofNullable(response.getBody());
    } catch (Exception e) {
      log.error("Error finding user: {}", username, e);
      throw handleException(e);
    }
  }

  @CircuitBreaker(name = "userProvider", fallbackMethod = "validateCredentialsFallback")
  public Optional<UserResponse> validateCredentials(
      String endpoint, String username, String password, Set<String> scopes)
      throws UserProviderException {
    try {

      var requestBuilder = restClient.post().uri(endpoint);

      requestBuilder = setUpHeaders(requestBuilder);

      Map<String, Object> body = new java.util.HashMap<>();
      body.put("username", username);
      body.put("password", password);
      body.put("scopes", scopes != null ? scopes : Collections.emptySet());

      ResponseEntity<UserResponse> response =
          requestBuilder
              .body(body)
              .retrieve()
              .onStatus(
                  status -> status.value() == HttpStatus.UNAUTHORIZED.value(),
                  (request, responseEntity) -> {
                    log.debug("Invalid credentials for user: {}", username);
                  })
              .toEntity(UserResponse.class);

      return Optional.ofNullable(response.getBody());
    } catch (Exception e) {
      log.error("Error validating credentials for user: {}", username, e);
      throw handleException(e);
    }
  }

  @CircuitBreaker(name = "userProvider", fallbackMethod = "validateScopesFallback")
  public boolean validateScopes(String endpoint, Set<String> scopes) throws UserProviderException {
    try {
      var requestBuilder = restClient.post().uri(endpoint);

      requestBuilder = setUpHeaders(requestBuilder);

      requestBuilder
          .body(Map.of("scopes", scopes != null ? scopes : Collections.emptySet()))
          .retrieve()
          .onStatus(
              status -> status.value() == HttpStatus.FORBIDDEN.value(),
              (request, response) -> {
                log.debug("Scopes validation rejected (403 Forbidden)");
              })
          .onStatus(
              status -> status.isError() && status.value() != HttpStatus.FORBIDDEN.value(),
              (request, response) -> {
                log.error("Error validating scopes: HTTP {}", response.getStatusCode());
              })
          .toBodilessEntity();

      return true;
    } catch (HttpClientErrorException e) {
      if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
        return false;
      }
      throw handleException(e);
    } catch (Exception e) {
      log.error("Error validating scopes", e);
      throw handleException(e);
    }
  }

  /**
   * The endpoint URL must have the {userId} placeholder already replaced since we cannot make the
   * user ID available in the circuit breaker fallback method signature.
   */
  @CircuitBreaker(name = "userProvider", fallbackMethod = "registerMfaFallback")
  public boolean registerMfa(String mfaEndpoint, String mfaSecret, String mfaKeyId)
      throws UserProviderException {
    try {
      Map<String, Object> requestBody =
          Map.of(
              "mfaSecret", mfaSecret,
              "twoFaRegistered", true,
              "mfaKeyId", mfaKeyId);

      var requestBuilder = restClient.post().uri(mfaEndpoint).body(requestBody);

      requestBuilder = setUpHeaders(requestBuilder);

      requestBuilder
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
      throw handleException(e);
    }
  }

  /**
   * Fails fast with a specific error rather than returning Optional.empty() to ensure callers know
   * authentication failed due to service unavailability, not invalid credentials.
   */
  Optional<UserResponse> findByUsernameFallback(
      String endpoint, String username, Exception exception) throws UserProviderException {
    log.warn("Circuit breaker activated for findByUsername - user provider service unavailable");
    throw new UserProviderException(
        List.of("error.code.circuit-breaker.open"),
        List.of("User provider service temporarily unavailable - circuit breaker activated"),
        Instant.now(),
        exception);
  }

  /**
   * Fails fast with a specific error rather than returning Optional.empty() to ensure callers know
   * validation failed due to service unavailability, not invalid credentials.
   */
  Optional<UserResponse> validateCredentialsFallback(
      String endpoint, String username, String password, Set<String> scopes, Exception exception)
      throws UserProviderException {
    log.warn(
        "Circuit breaker activated for validateCredentials - user provider service unavailable");
    throw new UserProviderException(
        List.of("error.code.circuit-breaker.open"),
        List.of("User provider service temporarily unavailable - circuit breaker activated"),
        Instant.now(),
        exception);
  }

  boolean validateScopesFallback(String endpoint, Set<String> scopes, Exception exception)
      throws UserProviderException {
    log.warn("Circuit breaker activated for validateScopes - user provider service unavailable");
    throw new UserProviderException(
        List.of("error.code.circuit-breaker.open"),
        List.of("User provider service temporarily unavailable - circuit breaker activated"),
        Instant.now(),
        exception);
  }

  /**
   * Fails fast to ensure users know MFA setup failed due to service unavailability, preventing
   * incomplete security configurations.
   */
  boolean registerMfaFallback(
      String mfaEndpoint, String mfaSecret, String mfaKeyId, Exception exception)
      throws UserProviderException {
    log.warn("Circuit breaker activated for registerMfa - user provider service unavailable");
    throw new UserProviderException(
        List.of("error.code.circuit-breaker.open"),
        List.of("User provider service temporarily unavailable - circuit breaker activated"),
        Instant.now(),
        exception);
  }

  private UserProviderException handleException(Exception exception) {
    try {
      if (exception instanceof HttpClientErrorException httpClientException) {
        // Try to parse the error response body
        String errorBody = httpClientException.getResponseBodyAsString();
        if (errorBody != null && !errorBody.isEmpty()) {
          try {
            UserProviderErrorResponse errorResponse =
                objectMapper.readValue(errorBody, UserProviderErrorResponse.class);

            if (httpClientException.getStatusCode().is4xxClientError()) {
              return new UserProviderClientException(
                  errorResponse.getErrorCodes(),
                  errorResponse.getMessages(),
                  errorResponse.getErrorDate(),
                  exception);
            }

            return new UserProviderException(
                errorResponse.getErrorCodes(),
                errorResponse.getMessages(),
                errorResponse.getErrorDate(),
                exception);
          } catch (Exception parsingException) {
            log.warn("Failed to parse error response from User Provider API", parsingException);
          }
        }
      }
    } catch (Exception e) {
      log.warn("Error handling exception for User Provider API", e);
    }

    // Fallback to generic error
    return new UserProviderException(
        List.of("error.code.user-provider.service-down"),
        List.of("User provider service is temporarily unavailable"),
        Instant.now(),
        exception);
  }

  @SuppressWarnings("unchecked")
  private <T extends RestClient.RequestHeadersSpec<?>> T setUpHeaders(
      RestClient.RequestHeadersSpec<?> requestBuilder) {
    if (apiKeyProperties.isMustBeValidated() && apiKeyProperties.getValue() != null) {
      return (T)
          requestBuilder.header(apiKeyProperties.getHeaderName(), apiKeyProperties.getValue());
    }
    return (T) requestBuilder;
  }
}
