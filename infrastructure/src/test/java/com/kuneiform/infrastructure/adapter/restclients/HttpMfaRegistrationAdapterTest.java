package com.kuneiform.infrastructure.adapter.restclients;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kuneiform.domain.exception.UserProviderException;
import com.kuneiform.domain.model.OAuthClient;
import com.kuneiform.domain.model.Tenant;
import com.kuneiform.domain.model.UserProvider;
import com.kuneiform.domain.port.ClientRepository;
import com.kuneiform.domain.port.TenantRepository;
import com.kuneiform.infrastructure.adapter.HttpMfaRegistrationAdapter;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for HttpMfaRegistrationAdapter.
 *
 * <p>Tests the adapter's functionality including: - MFA registration with user provider - Dynamic
 * endpoint resolution from clientId via tenant - Error handling
 */
@ExtendWith(MockitoExtension.class)
class HttpMfaRegistrationAdapterTest {

  @Mock private UserProviderRestClient restClient;
  @Mock private ClientRepository clientRepository;
  @Mock private TenantRepository tenantRepository;

  private HttpMfaRegistrationAdapter adapter;

  @BeforeEach
  void setUp() {
    adapter = new HttpMfaRegistrationAdapter(restClient, clientRepository, tenantRepository);
  }

  @Test
  void registerMfa_shouldSuccessfullyRegisterWithUserProvider() throws UserProviderException {
    // Given: Client with configured user provider via tenant
    String clientId = "test-client";
    String tenantId = "test-tenant";
    String userId = "user-123";
    String mfaSecret = "JBSWY3DPEHPK3PXP";
    String mfaKeyId = "WedgeAuth:user@example.com";
    String mfaEndpoint = "http://localhost:8081/api/users/{userId}/mfa";

    OAuthClient client = OAuthClient.builder().clientId(clientId).tenantId(tenantId).build();

    UserProvider userProvider =
        UserProvider.builder()
            .endpoint("http://localhost:8081/api/users/validate")
            .timeout(5000)
            .mfaRegistrationEndpoint(mfaEndpoint)
            .build();

    Tenant tenant = Tenant.builder().id(tenantId).userProvider(userProvider).build();

    when(clientRepository.findByClientId(clientId)).thenReturn(Optional.of(client));
    when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
    doReturn(true).when(restClient).registerMfa(anyString(), eq(mfaSecret), eq(mfaKeyId));

    // When: Registering MFA
    boolean result = adapter.registerMfa(clientId, userId, mfaSecret, mfaKeyId);

    // Then: Registration is successful
    assertThat(result).isTrue();

    // Verify endpoint resolution (template with {userId} replaced)
    String expectedEndpoint = "http://localhost:8081/api/users/" + userId + "/mfa";
    verify(restClient).registerMfa(expectedEndpoint, mfaSecret, mfaKeyId);
  }

  @Test
  void registerMfa_shouldHandleEndpointTemplateCorrectly() throws UserProviderException {
    // Given: MFA endpoint with {userId} placeholder
    String clientId = "test-client";
    String tenantId = "test-tenant";
    String userId = "abc-123-def";
    String mfaSecret = "SECRETKEY";
    String mfaKeyId = "WedgeAuth:test@test.com";
    String mfaEndpointTemplate = "https://api.example.com/users/{userId}/2fa";

    OAuthClient client = OAuthClient.builder().clientId(clientId).tenantId(tenantId).build();

    UserProvider userProvider =
        UserProvider.builder()
            .endpoint("https://api.example.com/users/validate")
            .timeout(3000)
            .mfaRegistrationEndpoint(mfaEndpointTemplate)
            .build();

    Tenant tenant = Tenant.builder().id(tenantId).userProvider(userProvider).build();

    when(clientRepository.findByClientId(clientId)).thenReturn(Optional.of(client));
    when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
    doReturn(true).when(restClient).registerMfa(anyString(), anyString(), anyString());

    // When: Registering MFA
    adapter.registerMfa(clientId, userId, mfaSecret, mfaKeyId);

    // Then: {userId} is replaced correctly
    String expectedEndpoint = "https://api.example.com/users/" + userId + "/2fa";
    verify(restClient).registerMfa(expectedEndpoint, mfaSecret, mfaKeyId);
  }

  @Test
  void registerMfa_shouldReturnFalseWhenRegistrationFails() throws UserProviderException {
    // Given: MFA registration fails
    String clientId = "test-client";
    String tenantId = "test-tenant";
    String userId = "user-123";
    String mfaSecret = "JBSWY3DPEHPK3PXP";
    String mfaKeyId = "WedgeAuth:user@example.com";
    String mfaEndpoint = "http://localhost:8081/api/users/{userId}/mfa";

    OAuthClient client = OAuthClient.builder().clientId(clientId).tenantId(tenantId).build();

    UserProvider userProvider =
        UserProvider.builder()
            .endpoint("http://localhost:8081/api/users/validate")
            .timeout(5000)
            .mfaRegistrationEndpoint(mfaEndpoint)
            .build();

    Tenant tenant = Tenant.builder().id(tenantId).userProvider(userProvider).build();

    when(clientRepository.findByClientId(clientId)).thenReturn(Optional.of(client));
    when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
    doReturn(false)
        .when(restClient)
        .registerMfa(anyString(), anyString(), anyString()); // Registration fails

    // When: Registering MFA
    boolean result = adapter.registerMfa(clientId, userId, mfaSecret, mfaKeyId);

    // Then: Registration failure is returned
    assertThat(result).isFalse();
  }

  @Test
  void registerMfa_shouldReturnFalseWhenClientNotFound() {
    // Given: Client not found
    String clientId = "unknown-client";

    when(clientRepository.findByClientId(clientId)).thenReturn(Optional.empty());

    // When: Registering MFA
    boolean result = adapter.registerMfa(clientId, "user-123", "SECRET", "WedgeAuth:user");

    // Then: Registration fails
    assertThat(result).isFalse();
  }

  @Test
  void registerMfa_shouldReturnFalseWhenTenantNotConfigured() {
    // Given: Client without tenant
    String clientId = "test-client";

    OAuthClient client =
        OAuthClient.builder()
            .clientId(clientId)
            .tenantId(null) // No tenant
            .build();

    when(clientRepository.findByClientId(clientId)).thenReturn(Optional.of(client));

    // When: Registering MFA
    boolean result = adapter.registerMfa(clientId, "user-123", "SECRET", "WedgeAuth:user");

    // Then: Registration fails
    assertThat(result).isFalse();
  }

  @Test
  void registerMfa_shouldReturnFalseWhenTenantNotFound() {
    // Given: Client with non-existent tenant
    String clientId = "test-client";
    String tenantId = "unknown-tenant";

    OAuthClient client = OAuthClient.builder().clientId(clientId).tenantId(tenantId).build();

    when(clientRepository.findByClientId(clientId)).thenReturn(Optional.of(client));
    when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());

    // When: Registering MFA
    boolean result = adapter.registerMfa(clientId, "user-123", "SECRET", "WedgeAuth:user");

    // Then: Registration fails
    assertThat(result).isFalse();
  }

  @Test
  void registerMfa_shouldReturnFalseWhenUserProviderNotConfigured() {
    // Given: Tenant without user provider
    String clientId = "test-client";
    String tenantId = "test-tenant";

    OAuthClient client = OAuthClient.builder().clientId(clientId).tenantId(tenantId).build();

    Tenant tenant =
        Tenant.builder()
            .id(tenantId)
            .userProvider(null) // No user provider
            .build();

    when(clientRepository.findByClientId(clientId)).thenReturn(Optional.of(client));
    when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

    // When: Registering MFA
    boolean result = adapter.registerMfa(clientId, "user-123", "SECRET", "WedgeAuth:user");

    // Then: Registration fails
    assertThat(result).isFalse();
  }

  @Test
  void registerMfa_shouldReturnFalseWhenMfaEndpointNotConfigured() {
    // Given: User provider without MFA endpoint
    String clientId = "test-client";
    String tenantId = "test-tenant";
    String userId = "user-123";

    OAuthClient client = OAuthClient.builder().clientId(clientId).tenantId(tenantId).build();

    UserProvider userProvider =
        UserProvider.builder()
            .endpoint("http://localhost:8081/api/users/validate")
            .timeout(5000)
            .mfaRegistrationEndpoint(null) // No MFA endpoint!
            .build();

    Tenant tenant = Tenant.builder().id(tenantId).userProvider(userProvider).build();

    when(clientRepository.findByClientId(clientId)).thenReturn(Optional.of(client));
    when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

    // When: Registering MFA
    boolean result = adapter.registerMfa(clientId, userId, "SECRET", "WedgeAuth:user");

    // Then: Registration fails
    assertThat(result).isFalse();
  }

  @Test
  void registerMfa_shouldHandleComplexUserIds() throws UserProviderException {
    // Given: User ID with special characters
    String clientId = "test-client";
    String tenantId = "test-tenant";
    String userId = "user-abc-123-xyz-456";
    String mfaSecret = "SECRETKEY";
    String mfaKeyId = "WedgeAuth:complex@example.com";
    String mfaEndpoint = "http://localhost:8081/api/users/{userId}/mfa-setup";

    OAuthClient client = OAuthClient.builder().clientId(clientId).tenantId(tenantId).build();

    UserProvider userProvider =
        UserProvider.builder()
            .endpoint("http://localhost:8081/api/users/validate")
            .timeout(5000)
            .mfaRegistrationEndpoint(mfaEndpoint)
            .build();

    Tenant tenant = Tenant.builder().id(tenantId).userProvider(userProvider).build();

    when(clientRepository.findByClientId(clientId)).thenReturn(Optional.of(client));
    when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
    doReturn(true).when(restClient).registerMfa(anyString(), anyString(), anyString());

    // When: Registering MFA with complex user ID
    boolean result = adapter.registerMfa(clientId, userId, mfaSecret, mfaKeyId);

    // Then: User ID is correctly substituted
    assertThat(result).isTrue();
    String expectedEndpoint = "http://localhost:8081/api/users/" + userId + "/mfa-setup";
    verify(restClient).registerMfa(expectedEndpoint, mfaSecret, mfaKeyId);
  }

  @Test
  void registerMfa_shouldHandleMultipleUserIdPlaceholders() throws UserProviderException {
    // Given: Endpoint with multiple {userId} placeholders (edge case)
    String clientId = "test-client";
    String tenantId = "test-tenant";
    String userId = "user-123";
    String mfaSecret = "SECRET";
    String mfaKeyId = "WedgeAuth:user";
    String mfaEndpoint = "http://localhost:8081/{userId}/mfa/{userId}/setup";

    OAuthClient client = OAuthClient.builder().clientId(clientId).tenantId(tenantId).build();

    UserProvider userProvider =
        UserProvider.builder()
            .endpoint("http://localhost:8081/api/users/validate")
            .timeout(5000)
            .mfaRegistrationEndpoint(mfaEndpoint)
            .build();

    Tenant tenant = Tenant.builder().id(tenantId).userProvider(userProvider).build();

    when(clientRepository.findByClientId(clientId)).thenReturn(Optional.of(client));
    when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
    doReturn(true).when(restClient).registerMfa(anyString(), anyString(), anyString());

    // When: Registering MFA
    adapter.registerMfa(clientId, userId, mfaSecret, mfaKeyId);

    // Then: All {userId} placeholders are replaced
    String expectedEndpoint = "http://localhost:8081/" + userId + "/mfa/" + userId + "/setup";
    verify(restClient).registerMfa(expectedEndpoint, mfaSecret, mfaKeyId);
  }
}
