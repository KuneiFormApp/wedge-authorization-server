package com.kuneiform.infraestructure.adapter.restclients;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kuneiform.domain.model.OAuthClient;
import com.kuneiform.domain.model.Tenant;
import com.kuneiform.domain.model.User;
import com.kuneiform.domain.model.UserProvider;
import com.kuneiform.domain.port.ClientRepository;
import com.kuneiform.domain.port.TenantRepository;
import com.kuneiform.infraestructure.adapter.HttpUserProviderPortAdapter;
import com.kuneiform.infraestructure.adapter.models.MfaDataResponse;
import com.kuneiform.infraestructure.adapter.models.UserResponse;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for HttpUserProviderPortAdapter.
 *
 * <p>Tests the adapter's functionality including: - User validation with MFA data mapping - Error
 * handling - MfaData transformation from UserResponse - Tenant ID vs Client ID lookup logic
 */
@ExtendWith(MockitoExtension.class)
class HttpUserProviderPortAdapterTest {

  @Mock private UserProviderRestClient restClient;
  @Mock private ClientRepository clientRepository;
  @Mock private TenantRepository tenantRepository;

  private HttpUserProviderPortAdapter adapter;

  @BeforeEach
  void setUp() {
    adapter = new HttpUserProviderPortAdapter(restClient, clientRepository, tenantRepository);
  }

  @Test
  void validateUser_shouldUseTenantIdDirectlyWhenProvided() {
    // Given: Tenant ID is provided directly
    String clientId = null; // or irrelevant
    String tenantId = "test-tenant-direct";
    String username = "user@example.com";
    String password = "password123";
    String endpoint = "http://localhost:8081/api/users/validate";

    UserProvider userProvider = UserProvider.builder().endpoint(endpoint).timeout(5000).build();
    Tenant tenant = Tenant.builder().id(tenantId).userProvider(userProvider).build();

    when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

    UserResponse userResponse =
        new UserResponse("user-123", username, "user@example.com", Map.of(), false, null);
    when(restClient.validateCredentials(endpoint, username, password))
        .thenReturn(Optional.of(userResponse));

    // When
    Optional<User> result = adapter.validateCredentials(clientId, tenantId, username, password);

    // Then
    assertThat(result).isPresent();
    verify(tenantRepository).findById(tenantId);
    verify(clientRepository, never()).findByClientId(any());
  }

  @Test
  void validateUser_shouldFallbackToClientIdWhenTenantIdMissing() {
    // Given: Tenant ID is null, but Client ID is provided
    String clientId = "test-client";
    String tenantId = null;
    String username = "user@example.com";
    String password = "password123";
    String endpoint = "http://localhost:8081/api/users/validate";
    String resolvedTenantId = "resolved-tenant-id";

    OAuthClient client =
        OAuthClient.builder().clientId(clientId).tenantId(resolvedTenantId).build();
    UserProvider userProvider = UserProvider.builder().endpoint(endpoint).timeout(5000).build();
    Tenant tenant = Tenant.builder().id(resolvedTenantId).userProvider(userProvider).build();

    when(clientRepository.findByClientId(clientId)).thenReturn(Optional.of(client));
    when(tenantRepository.findById(resolvedTenantId)).thenReturn(Optional.of(tenant));

    UserResponse userResponse =
        new UserResponse("user-123", username, "user@example.com", Map.of(), false, null);
    when(restClient.validateCredentials(endpoint, username, password))
        .thenReturn(Optional.of(userResponse));

    // When
    Optional<User> result = adapter.validateCredentials(clientId, tenantId, username, password);

    // Then
    assertThat(result).isPresent();
    verify(clientRepository).findByClientId(clientId);
    verify(tenantRepository).findById(resolvedTenantId);
  }

  @Test
  void validateUser_shouldReturnUserWithMfaDataWhenMfaEnabled() {
    // Given: Client with configured user provider via tenant
    String clientId = "test-client";
    String tenantId = null;
    String username = "user@example.com";
    String password = "password123";
    String endpoint = "http://localhost:8081/api/users/validate";
    String resolvedTenantId = "test-tenant";

    setupClientAndTenant(clientId, resolvedTenantId, endpoint, 5000);

    // User Response with MFA enabled and registered
    UserResponse userResponse =
        new UserResponse(
            "user-123",
            username,
            "user@example.com",
            Map.of("role", "admin"),
            true, // mfaEnabled
            new MfaDataResponse(
                true, // twoFaRegistered
                "WedgeAuth:user@example.com",
                "JBSWY3DPEHPK3PXP" // mfaSecret
                ));

    when(restClient.validateCredentials(endpoint, username, password))
        .thenReturn(Optional.of(userResponse));

    // When: Validating user credentials
    Optional<User> result = adapter.validateCredentials(clientId, tenantId, username, password);

    // Then: User is returned with MFA data properly mapped
    assertThat(result).isPresent();
    User user = result.get();
    assertThat(user.getMfaEnabled()).isTrue();
    assertThat(user.getMfaData()).isNotNull();
    assertThat(user.getMfaData().isTwoFaRegistered()).isTrue();
  }

  @Test
  void validateUser_shouldReturnEmptyWhenCredentialsInvalid() {
    // Given: Invalid credentials
    String clientId = "test-client";
    String tenantId = null;
    String username = "user@example.com";
    String password = "wrong-password";
    String endpoint = "http://localhost:8081/api/users/validate";
    String resolvedTenantId = "test-tenant";

    setupClientAndTenant(clientId, resolvedTenantId, endpoint, 5000);

    when(restClient.validateCredentials(endpoint, username, password)).thenReturn(Optional.empty());

    // When: Validating user credentials
    Optional<User> result = adapter.validateCredentials(clientId, tenantId, username, password);

    // Then: Empty result is returned
    assertThat(result).isEmpty();
  }

  @Test
  void validateUser_shouldReturnEmptyWhenClientNotFound() {
    // Given: Client not found
    String clientId = "unknown-client";
    String tenantId = null;

    when(clientRepository.findByClientId(clientId)).thenReturn(Optional.empty());

    // When: Validating user credentials
    Optional<User> result =
        adapter.validateCredentials(clientId, tenantId, "user@example.com", "password");

    // Then: Empty result is returned
    assertThat(result).isEmpty();
  }

  @Test
  void validateUser_shouldReturnEmptyWhenTenantNotFound_DirectLookup() {
    // Given: Direct Tenant Lookup fails
    String clientId = null;
    String tenantId = "unknown-tenant";

    when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());

    // When
    Optional<User> result =
        adapter.validateCredentials(clientId, tenantId, "user@example.com", "password");

    // Then
    assertThat(result).isEmpty();
  }

  @Test
  void validateUser_shouldReturnEmptyWhenBothMissing() {
    Optional<User> result = adapter.validateCredentials(null, null, "user", "pass");
    assertThat(result).isEmpty();
  }

  @Test
  void findByUsername_shouldReturnUserWhenFound() {
    // Given: User exists in user provider (fallback flow)
    String clientId = "test-client";
    String tenantId = null;
    String username = "user@example.com";
    String endpoint = "http://localhost:8081/api/users/validate";
    String resolvedTenantId = "test-tenant";

    setupClientAndTenant(clientId, resolvedTenantId, endpoint, 5000);

    UserResponse userResponse =
        new UserResponse("user-123", username, "user@example.com", Map.of(), false, null);

    when(restClient.findByUsername(endpoint, username)).thenReturn(Optional.of(userResponse));

    // When: Finding user by username
    Optional<User> result = adapter.findByUsername(clientId, tenantId, username);

    // Then: User is returned
    assertThat(result).isPresent();
    assertThat(result.get().getUsername()).isEqualTo(username);
  }

  // Helper method to set up client and tenant with user provider (for fallback
  // scenario)
  private void setupClientAndTenant(
      String clientId, String tenantId, String endpoint, int timeout) {
    OAuthClient client = OAuthClient.builder().clientId(clientId).tenantId(tenantId).build();

    UserProvider userProvider = UserProvider.builder().endpoint(endpoint).timeout(timeout).build();

    Tenant tenant = Tenant.builder().id(tenantId).userProvider(userProvider).build();

    when(clientRepository.findByClientId(clientId)).thenReturn(Optional.of(client));
    when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
  }
}
