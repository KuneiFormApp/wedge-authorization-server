package com.kuneiform.infrastructure.adapter.restclients;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.kuneiform.domain.exception.UserProviderClientException;
import com.kuneiform.domain.exception.UserProviderException;
import com.kuneiform.infrastructure.adapter.models.MfaDataResponse;
import com.kuneiform.infrastructure.adapter.models.UserProviderErrorResponse;
import com.kuneiform.infrastructure.adapter.models.UserResponse;
import com.kuneiform.infrastructure.config.CircuitBreakerProperties;
import com.kuneiform.infrastructure.config.UserProviderApiKeyProperties;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.*;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class UserProviderRestClientTest {

  @Mock private RestClient restClient;

  @Mock private RequestHeadersUriSpec requestHeadersUriSpec;

  @Mock private RequestBodyUriSpec requestBodyUriSpec;

  @Mock private RequestBodySpec requestBodySpec;

  @Mock private ResponseSpec responseSpec;

  @Mock private RequestHeadersSpec<?> requestHeadersSpecAfterHeader;

  @Mock private ObjectMapper objectMapper;

  @Mock private CircuitBreakerProperties circuitBreakerProperties;

  @Mock private UserProviderApiKeyProperties apiKeyProperties;

  private UserProviderRestClient userProviderRestClient;

  @BeforeEach
  void setUp() {
    userProviderRestClient = new UserProviderRestClient(restClient, objectMapper, apiKeyProperties);
    clearInvocations(restClient);
    clearInvocations(requestHeadersUriSpec);
    clearInvocations(requestBodyUriSpec);
    clearInvocations(requestBodySpec);
    clearInvocations(responseSpec);
    clearInvocations(requestHeadersSpecAfterHeader);
    clearInvocations(objectMapper);
    clearInvocations(circuitBreakerProperties);
    clearInvocations(apiKeyProperties);
  }

  @Test
  void findByUsername_shouldReturnUserWhenFound() throws UserProviderException {
    // Given
    String endpoint = "http://localhost:8081/api/users/validate";
    String username = "testuser";
    UserResponse expectedResponse =
        new UserResponse("user123", username, "test@example.com", Map.of(), false, null);

    when(restClient.get()).thenReturn(requestHeadersUriSpec);
    when(requestHeadersUriSpec.uri(any(String.class), eq(username)))
        .thenReturn(requestHeadersUriSpec);
    when(apiKeyProperties.isMustBeValidated()).thenReturn(false);
    when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.toEntity(UserResponse.class)).thenReturn(ResponseEntity.ok(expectedResponse));

    // When
    Optional<UserResponse> result = userProviderRestClient.findByUsername(endpoint, username);

    // Then
    assertTrue(result.isPresent());
    assertEquals(expectedResponse, result.get());
  }

  @Test
  void findByUsername_shouldReturnEmptyWhenNotFound() throws UserProviderException {
    // Given
    String endpoint = "http://localhost:8081/api/users/validate";
    String username = "nonexistent";

    when(restClient.get()).thenReturn(requestHeadersUriSpec);
    when(requestHeadersUriSpec.uri(any(String.class), eq(username)))
        .thenReturn(requestHeadersUriSpec);
    when(apiKeyProperties.isMustBeValidated()).thenReturn(false);
    when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.toEntity(UserResponse.class)).thenReturn(ResponseEntity.ok().body(null));

    // When
    Optional<UserResponse> result = userProviderRestClient.findByUsername(endpoint, username);

    // Then
    assertFalse(result.isPresent());
  }

  @Test
  void findByUsername_shouldAddApiKeyHeaderWhenEnabledAndPresent() throws UserProviderException {
    // Given
    String endpoint = "http://localhost:8081/api/users/validate";
    String username = "testuser";
    String apiKeyHeaderName = "X-API-KEY";
    String apiKeyValue = "test-api-key";
    UserResponse expectedResponse =
        new UserResponse("user123", username, "test@example.com", Map.of(), false, null);

    when(restClient.get()).thenReturn(requestHeadersUriSpec);
    when(requestHeadersUriSpec.uri(any(String.class), eq(username)))
        .thenReturn(requestHeadersUriSpec);
    when(apiKeyProperties.isMustBeValidated()).thenReturn(true);
    when(apiKeyProperties.getHeaderName()).thenReturn(apiKeyHeaderName);
    when(apiKeyProperties.getValue()).thenReturn(apiKeyValue);
    when(requestHeadersUriSpec.header(apiKeyHeaderName, apiKeyValue))
        .thenReturn(requestHeadersSpecAfterHeader);
    when(requestHeadersSpecAfterHeader.retrieve()).thenReturn(responseSpec);
    when(responseSpec.toEntity(UserResponse.class)).thenReturn(ResponseEntity.ok(expectedResponse));

    // When
    Optional<UserResponse> result = userProviderRestClient.findByUsername(endpoint, username);

    // Then
    assertTrue(result.isPresent());
    assertEquals(expectedResponse, result.get());
    verify(apiKeyProperties, times(1)).getHeaderName();
    verify(apiKeyProperties, times(2)).getValue();
  }

  @Test
  void findByUsername_shouldNotAddApiKeyHeaderWhenDisabled() throws UserProviderException {
    // Given
    String endpoint = "http://localhost:8081/api/users/validate";
    String username = "testuser";
    UserResponse expectedResponse =
        new UserResponse("user123", username, "test@example.com", Map.of(), false, null);

    when(restClient.get()).thenReturn(requestHeadersUriSpec);
    when(requestHeadersUriSpec.uri(any(String.class), eq(username)))
        .thenReturn(requestHeadersUriSpec);
    when(apiKeyProperties.isMustBeValidated()).thenReturn(false);
    when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.toEntity(UserResponse.class)).thenReturn(ResponseEntity.ok(expectedResponse));

    // When
    Optional<UserResponse> result = userProviderRestClient.findByUsername(endpoint, username);

    // Then
    assertTrue(result.isPresent());
    assertEquals(expectedResponse, result.get());
    verify(requestHeadersUriSpec, never()).header(anyString(), anyString());
  }

  @Test
  void findByUsername_shouldNotAddApiKeyHeaderWhenEnabledButValueIsNull()
      throws UserProviderException {
    // Given
    String endpoint = "http://localhost:8081/api/users/validate";
    String username = "testuser";
    UserResponse expectedResponse =
        new UserResponse("user123", username, "test@example.com", Map.of(), false, null);

    when(restClient.get()).thenReturn(requestHeadersUriSpec);
    when(requestHeadersUriSpec.uri(any(String.class), eq(username)))
        .thenReturn(requestHeadersUriSpec);
    when(apiKeyProperties.isMustBeValidated()).thenReturn(true);
    when(apiKeyProperties.getValue()).thenReturn(null);
    when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.toEntity(UserResponse.class)).thenReturn(ResponseEntity.ok(expectedResponse));

    // When
    Optional<UserResponse> result = userProviderRestClient.findByUsername(endpoint, username);

    // Then
    assertTrue(result.isPresent());
    assertEquals(expectedResponse, result.get());
    verify(requestHeadersUriSpec, never()).header(anyString(), anyString());
  }

  @Test
  void validateCredentials_shouldReturnUserWhenValid() throws UserProviderException {
    // Given
    String endpoint = "http://localhost:8081/api/users/validate";
    String username = "testuser";
    String password = "password123";

    MfaDataResponse mfaData = new MfaDataResponse(false, "WedgeAuth:testuser", null);
    UserResponse expectedResponse =
        new UserResponse("user123", username, "test@example.com", Map.of(), true, mfaData);

    when(restClient.post()).thenReturn(requestBodyUriSpec);
    when(requestBodyUriSpec.uri(endpoint)).thenReturn(requestBodySpec);
    when(apiKeyProperties.isMustBeValidated()).thenReturn(false);
    when(requestBodySpec.body(any(Map.class))).thenReturn(requestBodySpec);
    when(requestBodySpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
    when(responseSpec.toEntity(UserResponse.class)).thenReturn(ResponseEntity.ok(expectedResponse));

    // When
    Optional<UserResponse> result =
        userProviderRestClient.validateCredentials(
            endpoint, username, password, Collections.emptySet());

    // Then
    assertTrue(result.isPresent());
    assertEquals(expectedResponse, result.get());
    assertTrue(result.get().mfaEnabled());
    assertNotNull(result.get().mfaData());
  }

  @Test
  void registerMfa_shouldReturnTrueOnSuccess() throws UserProviderException {
    // Given
    String mfaEndpoint = "http://localhost:8081/api/users/user123/mfa";
    String mfaSecret = "JBSWY3DPEHPK3PXP";
    String mfaKeyId = "WedgeAuth:user@example.com";

    when(restClient.post()).thenReturn(requestBodyUriSpec);
    when(requestBodyUriSpec.uri(mfaEndpoint)).thenReturn(requestBodySpec);
    when(apiKeyProperties.isMustBeValidated()).thenReturn(false);
    when(requestBodySpec.body(any(Map.class))).thenReturn(requestBodySpec);
    when(requestBodySpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
    when(responseSpec.toBodilessEntity()).thenReturn(ResponseEntity.noContent().build());

    // When
    boolean result = userProviderRestClient.registerMfa(mfaEndpoint, mfaSecret, mfaKeyId);

    // Then
    assertTrue(result);
    verify(restClient).post();
  }

  @Test
  void registerMfa_shouldThrowOnException() throws UserProviderException {
    // Given
    String mfaEndpoint = "http://localhost:8081/api/users/user123/mfa";
    String mfaSecret = "SECRET";
    String mfaKeyId = "App:user";

    when(restClient.post()).thenThrow(new RuntimeException("Network error"));

    // When & Then
    UserProviderException exception =
        assertThrows(
            UserProviderException.class,
            () -> userProviderRestClient.registerMfa(mfaEndpoint, mfaSecret, mfaKeyId));

    assertEquals("error.code.user-provider.service-down", exception.getErrorCodes().get(0));
    assertTrue(exception.getMessages().get(0).contains("unavailable"));
  }

  @Test
  void findByUsername_shouldThrowUserProviderExceptionWhenErrorResponse()
      throws UserProviderException {
    // Given
    String endpoint = "http://localhost:8081/api/users/validate";
    String username = "testuser";
    String errorResponseJson =
        """
        {
          "errorCodes": ["USER_NOT_FOUND"],
          "messages": ["User not found"],
          "errorDate": "2026-01-22T17:58:14Z"
        }
        """;

    HttpClientErrorException httpException =
        HttpClientErrorException.create(
            HttpStatus.NOT_FOUND, "Not Found", null, errorResponseJson.getBytes(), null);

    UserProviderErrorResponse errorResponse =
        UserProviderErrorResponse.builder()
            .errorCodes(List.of("USER_NOT_FOUND"))
            .messages(List.of("User not found"))
            .errorDate(Instant.parse("2026-01-22T17:58:14Z"))
            .build();

    when(restClient.get()).thenReturn(requestHeadersUriSpec);
    when(requestHeadersUriSpec.uri(any(String.class), eq(username)))
        .thenReturn(requestHeadersUriSpec);
    when(apiKeyProperties.isMustBeValidated()).thenReturn(false);
    when(requestHeadersUriSpec.retrieve()).thenThrow(httpException);
    when(objectMapper.readValue(eq(errorResponseJson), eq(UserProviderErrorResponse.class)))
        .thenReturn(errorResponse);

    // When & Then
    UserProviderClientException exception =
        assertThrows(
            UserProviderClientException.class,
            () -> userProviderRestClient.findByUsername(endpoint, username));

    assertEquals(List.of("USER_NOT_FOUND"), exception.getErrorCodes());
    assertEquals(List.of("User not found"), exception.getMessages());
    assertEquals(Instant.parse("2026-01-22T17:58:14Z"), exception.getErrorDate());
  }

  @Test
  void validateCredentials_shouldThrowUserProviderExceptionWhenInvalidCredentials()
      throws UserProviderException {
    // Given
    String endpoint = "http://localhost:8081/api/users/validate";
    String username = "testuser";
    String password = "wrongpassword";
    String errorResponseJson =
        """
        {
          "errorCodes": ["INVALID_CREDENTIALS"],
          "messages": ["Invalid username or password"],
          "errorDate": "2026-01-22T17:58:14Z"
        }
        """;

    HttpClientErrorException httpException =
        HttpClientErrorException.create(
            HttpStatus.UNAUTHORIZED, "Unauthorized", null, errorResponseJson.getBytes(), null);

    UserProviderErrorResponse errorResponse =
        UserProviderErrorResponse.builder()
            .errorCodes(List.of("INVALID_CREDENTIALS"))
            .messages(List.of("Invalid username or password"))
            .errorDate(Instant.parse("2026-01-22T17:58:14Z"))
            .build();

    when(restClient.post()).thenReturn(requestBodyUriSpec);
    when(requestBodyUriSpec.uri(endpoint)).thenReturn(requestBodySpec);
    when(apiKeyProperties.isMustBeValidated()).thenReturn(false);
    when(requestBodySpec.body(any(Map.class))).thenReturn(requestBodySpec);
    when(requestBodySpec.retrieve()).thenThrow(httpException);
    when(objectMapper.readValue(eq(errorResponseJson), eq(UserProviderErrorResponse.class)))
        .thenReturn(errorResponse);

    // When & Then
    UserProviderClientException exception =
        assertThrows(
            UserProviderClientException.class,
            () ->
                userProviderRestClient.validateCredentials(
                    endpoint, username, password, Collections.emptySet()));

    assertEquals(List.of("INVALID_CREDENTIALS"), exception.getErrorCodes());
    assertEquals(List.of("Invalid username or password"), exception.getMessages());
    assertEquals(Instant.parse("2026-01-22T17:58:14Z"), exception.getErrorDate());
  }

  @Test
  void registerMfa_shouldThrowUserProviderExceptionWhenMfaRegistrationFails()
      throws UserProviderException {
    // Given
    String mfaEndpoint = "http://localhost:8081/api/users/user123/mfa";
    String mfaSecret = "JBSWY3DPEHPK3PXP";
    String mfaKeyId = "WedgeAuth:user@example.com";
    String errorResponseJson =
        """
        {
          "errorCodes": ["MFA_REGISTRATION_FAILED"],
          "messages": ["Failed to register MFA"],
          "errorDate": "2026-01-22T17:58:14Z"
        }
        """;

    HttpClientErrorException httpException =
        HttpClientErrorException.create(
            HttpStatus.BAD_REQUEST, "Bad Request", null, errorResponseJson.getBytes(), null);

    UserProviderErrorResponse errorResponse =
        UserProviderErrorResponse.builder()
            .errorCodes(List.of("MFA_REGISTRATION_FAILED"))
            .messages(List.of("Failed to register MFA"))
            .errorDate(Instant.parse("2026-01-22T17:58:14Z"))
            .build();

    when(restClient.post()).thenReturn(requestBodyUriSpec);
    when(requestBodyUriSpec.uri(mfaEndpoint)).thenReturn(requestBodySpec);
    when(apiKeyProperties.isMustBeValidated()).thenReturn(false);
    when(requestBodySpec.body(any(Map.class))).thenReturn(requestBodySpec);
    when(requestBodySpec.retrieve()).thenThrow(httpException);
    when(objectMapper.readValue(eq(errorResponseJson), eq(UserProviderErrorResponse.class)))
        .thenReturn(errorResponse);

    // When & Then
    UserProviderClientException exception =
        assertThrows(
            UserProviderClientException.class,
            () -> userProviderRestClient.registerMfa(mfaEndpoint, mfaSecret, mfaKeyId));

    assertEquals(List.of("MFA_REGISTRATION_FAILED"), exception.getErrorCodes());
    assertEquals(List.of("Failed to register MFA"), exception.getMessages());
    assertEquals(Instant.parse("2026-01-22T17:58:14Z"), exception.getErrorDate());
  }

  @Test
  void findByUsernameFallback_shouldThrowCircuitBreakerException() throws UserProviderException {
    // Given
    String endpoint = "http://localhost:8081/api/users/validate";
    String username = "testuser";
    Exception fallbackException = new RuntimeException("Circuit breaker open");

    // When & Then
    UserProviderException exception =
        assertThrows(
            UserProviderException.class,
            () ->
                userProviderRestClient.findByUsernameFallback(
                    endpoint, username, fallbackException));

    assertEquals(List.of("error.code.circuit-breaker.open"), exception.getErrorCodes());
    assertTrue(exception.getMessages().get(0).contains("circuit breaker activated"));
    assertNotNull(exception.getErrorDate());
  }

  @Test
  void validateCredentialsFallback_shouldThrowCircuitBreakerException()
      throws UserProviderException {
    // Given
    String endpoint = "http://localhost:8081/api/users/validate";
    String username = "testuser";
    String password = "password123";
    Exception fallbackException = new RuntimeException("Circuit breaker open");

    // When & Then
    UserProviderException exception =
        assertThrows(
            UserProviderException.class,
            () ->
                userProviderRestClient.validateCredentialsFallback(
                    endpoint, username, password, Collections.emptySet(), fallbackException));

    assertEquals(List.of("error.code.circuit-breaker.open"), exception.getErrorCodes());
    assertTrue(exception.getMessages().get(0).contains("circuit breaker activated"));
    assertNotNull(exception.getErrorDate());
  }

  @Test
  void validateScopes_shouldReturnTrueWhenOk() throws UserProviderException {
    // Given
    String endpoint = "http://localhost:8081/api/users/user123/scopes";
    Set<String> scopes = Set.of("openid", "profile");

    when(restClient.post()).thenReturn(requestBodyUriSpec);
    when(requestBodyUriSpec.uri(endpoint)).thenReturn(requestBodySpec);
    when(apiKeyProperties.isMustBeValidated()).thenReturn(false);
    when(requestBodySpec.body(any(Map.class))).thenReturn(requestBodySpec);
    when(requestBodySpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
    when(responseSpec.toBodilessEntity()).thenReturn(ResponseEntity.ok().build());

    // When
    boolean result = userProviderRestClient.validateScopes(endpoint, scopes);

    // Then
    assertTrue(result);
  }

  @Test
  void validateScopes_shouldReturnFalseWhenForbidden() throws UserProviderException {
    // Given
    String endpoint = "http://localhost:8081/api/users/user123/scopes";
    Set<String> scopes = Set.of("admin");

    HttpClientErrorException forbiddenException =
        HttpClientErrorException.create(HttpStatus.FORBIDDEN, "Forbidden", null, null, null);

    when(restClient.post()).thenReturn(requestBodyUriSpec);
    when(requestBodyUriSpec.uri(endpoint)).thenReturn(requestBodySpec);
    when(apiKeyProperties.isMustBeValidated()).thenReturn(false);
    when(requestBodySpec.body(any(Map.class))).thenReturn(requestBodySpec);
    when(requestBodySpec.retrieve()).thenThrow(forbiddenException);

    // When
    boolean result = userProviderRestClient.validateScopes(endpoint, scopes);

    // Then
    assertFalse(result);
  }

  @Test
  void validateScopesFallback_shouldThrowCircuitBreakerException() {
    // Given
    String endpoint = "http://localhost:8081/api/users/user123/scopes";
    Set<String> scopes = Set.of("openid");
    Exception fallbackException = new RuntimeException("Circuit breaker open");

    // When & Then
    UserProviderException exception =
        assertThrows(
            UserProviderException.class,
            () ->
                userProviderRestClient.validateScopesFallback(endpoint, scopes, fallbackException));

    assertEquals(List.of("error.code.circuit-breaker.open"), exception.getErrorCodes());
    assertTrue(exception.getMessages().get(0).contains("circuit breaker activated"));
  }

  @Test
  void registerMfaFallback_shouldThrowCircuitBreakerException() throws UserProviderException {
    // Given
    String mfaEndpoint = "http://localhost:8081/api/users/user123/mfa";
    String mfaSecret = "JBSWY3DPEHPK3PXP";
    String mfaKeyId = "WedgeAuth:user@example.com";
    Exception fallbackException = new RuntimeException("Circuit breaker open");

    // When & Then
    UserProviderException exception =
        assertThrows(
            UserProviderException.class,
            () ->
                userProviderRestClient.registerMfaFallback(
                    mfaEndpoint, mfaSecret, mfaKeyId, fallbackException));

    assertEquals(List.of("error.code.circuit-breaker.open"), exception.getErrorCodes());
    assertTrue(exception.getMessages().get(0).contains("circuit breaker activated"));
    assertNotNull(exception.getErrorDate());
  }

  @Test
  void shouldThrowFallbackExceptionWhenErrorBodyCannotBeParsed() throws UserProviderException {
    // Given
    String endpoint = "http://localhost:8081/api/users/validate";
    String username = "testuser";

    HttpClientErrorException httpException =
        HttpClientErrorException.create(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Internal Server Error",
            null,
            "Invalid JSON".getBytes(),
            null);

    when(restClient.get()).thenReturn(requestHeadersUriSpec);
    when(requestHeadersUriSpec.uri(any(String.class), eq(username)))
        .thenReturn(requestHeadersUriSpec);
    when(apiKeyProperties.isMustBeValidated()).thenReturn(false);
    when(requestHeadersUriSpec.retrieve()).thenThrow(httpException);
    when(objectMapper.readValue(eq("Invalid JSON"), eq(UserProviderErrorResponse.class)))
        .thenThrow(new RuntimeException("Cannot parse JSON"));

    // When & Then
    UserProviderException exception =
        assertThrows(
            UserProviderException.class,
            () -> userProviderRestClient.findByUsername(endpoint, username));

    assertEquals(List.of("error.code.user-provider.service-down"), exception.getErrorCodes());
    assertEquals(
        List.of("User provider service is temporarily unavailable"), exception.getMessages());
    assertNotNull(exception.getErrorDate());
  }
}
