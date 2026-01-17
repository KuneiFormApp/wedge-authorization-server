package com.kuneiform.infraestructure.adapter.restclients;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.kuneiform.infraestructure.adapter.models.MfaDataResponse;
import com.kuneiform.infraestructure.adapter.models.UserResponse;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.*;

@ExtendWith(MockitoExtension.class)
class UserProviderRestClientTest {

  @Mock private RestClient restClient;

  @Mock private RequestHeadersUriSpec requestHeadersUriSpec;

  @Mock private RequestBodyUriSpec requestBodyUriSpec;

  @Mock private RequestBodySpec requestBodySpec;

  @Mock private ResponseSpec responseSpec;

  private UserProviderRestClient userProviderRestClient;

  @BeforeEach
  void setUp() {
    userProviderRestClient = new UserProviderRestClient(restClient);
  }

  @Test
  void findByUsername_shouldReturnUserWhenFound() {
    // Given
    String endpoint = "http://localhost:8081/api/users/validate";
    String username = "testuser";
    UserResponse expectedResponse =
        new UserResponse("user123", username, "test@example.com", Map.of(), false, null);

    when(restClient.get()).thenReturn(requestHeadersUriSpec);
    when(requestHeadersUriSpec.uri(anyString(), eq(username))).thenReturn(requestHeadersUriSpec);
    when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
    when(responseSpec.body(UserResponse.class)).thenReturn(expectedResponse);

    // When
    Optional<UserResponse> result = userProviderRestClient.findByUsername(endpoint, username);

    // Then
    assertTrue(result.isPresent());
    assertEquals(expectedResponse, result.get());
  }

  @Test
  void findByUsername_shouldReturnEmptyWhenNotFound() {
    // Given
    String endpoint = "http://localhost:8081/api/users/validate";
    String username = "nonexistent";

    when(restClient.get()).thenReturn(requestHeadersUriSpec);
    when(requestHeadersUriSpec.uri(anyString(), eq(username))).thenReturn(requestHeadersUriSpec);
    when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
    when(responseSpec.body(UserResponse.class)).thenReturn(null);

    // When
    Optional<UserResponse> result = userProviderRestClient.findByUsername(endpoint, username);

    // Then
    assertFalse(result.isPresent());
  }

  @Test
  void validateCredentials_shouldReturnUserWhenValid() {
    // Given
    String endpoint = "http://localhost:8081/api/users/validate";
    String username = "testuser";
    String password = "password123";

    MfaDataResponse mfaData = new MfaDataResponse(false, "WedgeAuth:testuser", null);
    UserResponse expectedResponse =
        new UserResponse("user123", username, "test@example.com", Map.of(), true, mfaData);

    when(restClient.post()).thenReturn(requestBodyUriSpec);
    when(requestBodyUriSpec.uri(endpoint)).thenReturn(requestBodySpec);
    when(requestBodySpec.body(any(Map.class))).thenReturn(requestBodySpec);
    when(requestBodySpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
    when(responseSpec.body(UserResponse.class)).thenReturn(expectedResponse);

    // When
    Optional<UserResponse> result =
        userProviderRestClient.validateCredentials(endpoint, username, password);

    // Then
    assertTrue(result.isPresent());
    assertEquals(expectedResponse, result.get());
    assertTrue(result.get().mfaEnabled());
    assertNotNull(result.get().mfaData());
  }

  @Test
  void registerMfa_shouldReturnTrueOnSuccess() {
    // Given
    String mfaEndpoint = "http://localhost:8081/api/users/user123/mfa";
    String mfaSecret = "JBSWY3DPEHPK3PXP";
    String mfaKeyId = "WedgeAuth:user@example.com";

    when(restClient.patch()).thenReturn(requestBodyUriSpec);
    when(requestBodyUriSpec.uri(mfaEndpoint)).thenReturn(requestBodySpec);
    when(requestBodySpec.body(any(Map.class))).thenReturn(requestBodySpec);
    when(requestBodySpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
    when(responseSpec.toBodilessEntity()).thenReturn(null);

    // When
    boolean result = userProviderRestClient.registerMfa(mfaEndpoint, mfaSecret, mfaKeyId);

    // Then
    assertTrue(result);
    verify(restClient).patch();
  }

  @Test
  void registerMfa_shouldReturnFalseOnException() {
    // Given
    String mfaEndpoint = "http://localhost:8081/api/users/user123/mfa";
    String mfaSecret = "SECRET";
    String mfaKeyId = "App:user";

    when(restClient.patch()).thenThrow(new RuntimeException("Network error"));

    // When
    boolean result = userProviderRestClient.registerMfa(mfaEndpoint, mfaSecret, mfaKeyId);

    // Then
    assertFalse(result);
  }
}
