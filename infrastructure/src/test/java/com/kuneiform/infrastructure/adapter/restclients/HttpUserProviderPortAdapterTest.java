package com.kuneiform.infrastructure.adapter.restclients;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kuneiform.domain.exception.UserProviderException;
import com.kuneiform.domain.model.OAuthClient;
import com.kuneiform.domain.model.Tenant;
import com.kuneiform.domain.model.User;
import com.kuneiform.domain.model.UserProvider;
import com.kuneiform.domain.port.ClientRepository;
import com.kuneiform.domain.port.TenantRepository;
import com.kuneiform.infrastructure.adapter.HttpUserProviderPortAdapter;
import com.kuneiform.infrastructure.adapter.models.MfaDataResponse;
import com.kuneiform.infrastructure.adapter.models.UserResponse;
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
  void validateUser_shouldUseTenantIdDirectlyWhenProvided() throws UserProviderException {
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
    doReturn(Optional.of(userResponse))
        .when(restClient)
        .validateCredentials(eq(endpoint), eq(username), eq(password), any());

    // When
    Optional<User> result =
        adapter.validateCredentials(clientId, tenantId, username, password, Collections.emptySet());

    // Then
    assertThat(result).isPresent();
    verify(tenantRepository).findById(tenantId);
    verify(clientRepository, never()).findByClientId(any());
  }

  @Test
  void validateUser_shouldFallbackToClientIdWhenTenantIdMissing() throws UserProviderException {
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
    doReturn(Optional.of(userResponse))
        .when(restClient)
        .validateCredentials(eq(endpoint), eq(username), eq(password), any());

    // When
    Optional<User> result =
        adapter.validateCredentials(clientId, tenantId, username, password, Collections.emptySet());

    // Then
    assertThat(result).isPresent();
    verify(clientRepository).findByClientId(clientId);
    verify(tenantRepository).findById(resolvedTenantId);
  }

  @Test
  void validateUser_shouldReturnUserWithMfaDataWhenMfaEnabled() throws UserProviderException {
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

    doReturn(Optional.of(userResponse))
        .when(restClient)
        .validateCredentials(eq(endpoint), eq(username), eq(password), any());

    // When: Validating user credentials
    Optional<User> result =
        adapter.validateCredentials(clientId, tenantId, username, password, Collections.emptySet());

    // Then: User is returned with MFA data properly mapped
    assertThat(result).isPresent();
    User user = result.get();
    assertThat(user.getMfaEnabled()).isTrue();
    assertThat(user.getMfaData()).isNotNull();
    assertThat(user.getMfaData().isTwoFaRegistered()).isTrue();
  }

  @Test
  void validateUser_shouldReturnEmptyWhenCredentialsInvalid() throws UserProviderException {
    // Given: Invalid credentials
    String clientId = "test-client";
    String tenantId = null;
    String username = "user@example.com";
    String password = "wrong-password";
    String endpoint = "http://localhost:8081/api/users/validate";
    String resolvedTenantId = "test-tenant";

    setupClientAndTenant(clientId, resolvedTenantId, endpoint, 5000);

    doReturn(Optional.empty())
        .when(restClient)
        .validateCredentials(eq(endpoint), eq(username), eq(password), any());

    // When: Validating user credentials
    Optional<User> result =
        adapter.validateCredentials(clientId, tenantId, username, password, Collections.emptySet());

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
        adapter.validateCredentials(
            clientId, tenantId, "user@example.com", "password", Collections.emptySet());

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
        adapter.validateCredentials(
            clientId, tenantId, "user@example.com", "password", Collections.emptySet());

    // Then
    assertThat(result).isEmpty();
  }

  @Test
  void validateUser_shouldReturnEmptyWhenBothMissing() {
    Optional<User> result =
        adapter.validateCredentials(null, null, "user", "pass", Collections.emptySet());
    assertThat(result).isEmpty();
  }

  @Test
  void findByUsername_shouldReturnUserWhenFound() throws UserProviderException {
    // Given: User exists in user provider (fallback flow)
    String clientId = "test-client";
    String tenantId = null;
    String username = "user@example.com";
    String endpoint = "http://localhost:8081/api/users/validate";
    String resolvedTenantId = "test-tenant";

    setupClientAndTenant(clientId, resolvedTenantId, endpoint, 5000);

    UserResponse userResponse =
        new UserResponse("user-123", username, "user@example.com", Map.of(), false, null);

    doReturn(Optional.of(userResponse)).when(restClient).findByUsername(endpoint, username);

    // When: Finding user by username
    Optional<User> result = adapter.findByUsername(clientId, tenantId, username);

    // Then: User is returned
    assertThat(result).isPresent();
    assertThat(result.get().getUsername()).isEqualTo(username);
  }

  @Test
  void registerMfa_shouldReturnTrueWhenRegistrationSucceeds() throws UserProviderException {
    // Given: Valid tenant and MFA data
    String clientId = "test-client";
    String tenantId = "test-tenant";
    String mfaSecret = "JBSWY3DPEHPK3PXP";
    String mfaKeyId = "key-123";
    String mfaEndpoint = "http://localhost:8081/api/users/user-123/mfa";

    setupTenantWithMfaEndpoint(tenantId, mfaEndpoint);

    when(restClient.registerMfa(mfaEndpoint, mfaSecret, mfaKeyId)).thenReturn(true);

    // When: Registering MFA
    boolean result = adapter.registerMfa(clientId, tenantId, "user-123", mfaSecret, mfaKeyId);

    // Then: Registration succeeds
    assertThat(result).isTrue();
    verify(restClient).registerMfa(mfaEndpoint, mfaSecret, mfaKeyId);
  }

  @Test
  void registerMfa_shouldUseTenantIdDirectlyWhenProvided() throws UserProviderException {
    // Given: Tenant ID is provided directly
    String clientId = null;
    String tenantId = "test-tenant-direct";
    String mfaSecret = "JBSWY3DPEHPK3PXP";
    String mfaKeyId = "key-123";
    String mfaEndpoint = "http://localhost:8081/api/users/user-123/mfa";

    setupTenantWithMfaEndpoint(tenantId, mfaEndpoint);

    when(restClient.registerMfa(mfaEndpoint, mfaSecret, mfaKeyId)).thenReturn(true);

    // When: Registering MFA
    boolean result = adapter.registerMfa(clientId, tenantId, "user-123", mfaSecret, mfaKeyId);

    // Then: Registration succeeds using direct tenant lookup
    assertThat(result).isTrue();
    verify(tenantRepository).findById(tenantId);
    verify(clientRepository, never()).findByClientId(any());
  }

  @Test
  void registerMfa_shouldReturnFalseWhenClientNotFound() {
    // Given: Client not found
    String clientId = "unknown-client";
    String tenantId = null;

    when(clientRepository.findByClientId(clientId)).thenReturn(Optional.empty());

    // When: Attempting to register MFA
    boolean result = adapter.registerMfa(clientId, tenantId, "user-123", "secret", "key");

    // Then: Registration fails
    assertThat(result).isFalse();
  }

  @Test
  void registerMfa_shouldReturnFalseWhenTenantNotFound_DirectLookup() {
    // Given: Direct tenant lookup fails
    String clientId = null;
    String tenantId = "unknown-tenant";

    when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());

    // When: Attempting to register MFA
    boolean result = adapter.registerMfa(clientId, tenantId, "user-123", "secret", "key");

    // Then: Registration fails
    assertThat(result).isFalse();
  }

  @Test
  void registerMfa_shouldReturnFalseWhenUserProviderThrowsException() throws UserProviderException {
    // Given: User provider throws an exception
    String clientId = "test-client";
    String tenantId = "test-tenant";
    String mfaSecret = "JBSWY3DPEHPK3PXP";
    String mfaKeyId = "key-123";
    String mfaEndpoint = "http://localhost:8081/api/users/user-123/mfa";

    setupTenantWithMfaEndpoint(tenantId, mfaEndpoint);

    doThrow(
            new UserProviderException(
                List.of("error.code.user-provider.service-down"),
                List.of("User provider service is temporarily unavailable"),
                java.time.Instant.now(),
                new RuntimeException("Service unavailable")))
        .when(restClient)
        .registerMfa(mfaEndpoint, mfaSecret, mfaKeyId);

    // When: Attempting to register MFA
    boolean result = adapter.registerMfa(clientId, tenantId, "user-123", mfaSecret, mfaKeyId);

    // Then: Registration fails
    assertThat(result).isFalse();
  }

  @Test
  void validateScopes_shouldReturnTrueWhenValidationSucceeds() throws UserProviderException {
    // Given
    String clientId = "test-client";
    String tenantId = "test-tenant";
    String userId = "user-123";
    Set<String> scopes = Set.of("openid", "profile");
    String scopesEndpoint = "http://localhost:8081/api/users/user-123/scopes";

    setupTenantWithScopesEndpoint(tenantId, scopesEndpoint);

    when(restClient.validateScopes(scopesEndpoint, scopes)).thenReturn(true);

    // When
    boolean result = adapter.validateScopes(clientId, tenantId, userId, scopes);

    // Then
    assertThat(result).isTrue();
    verify(restClient).validateScopes(scopesEndpoint, scopes);
  }

  @Test
  void validateScopes_shouldReturnFalseWhenValidationFails() throws UserProviderException {
    // Given
    String clientId = "test-client";
    String tenantId = "test-tenant";
    String userId = "user-123";
    Set<String> scopes = Set.of("admin");
    String scopesEndpoint = "http://localhost:8081/api/users/user-123/scopes";

    setupTenantWithScopesEndpoint(tenantId, scopesEndpoint);

    when(restClient.validateScopes(scopesEndpoint, scopes)).thenReturn(false);

    // When
    boolean result = adapter.validateScopes(clientId, tenantId, userId, scopes);

    // Then
    assertThat(result).isFalse();
  }

  @Test
  void validateScopes_shouldReturnTrueWhenEndpointNotConfigured() throws UserProviderException {
    // Given
    String clientId = "test-client";
    String tenantId = "test-tenant";
    String userId = "user-123";
    Set<String> scopes = Set.of("openid");

    UserProvider userProvider =
        UserProvider.builder().endpoint("http://localhost:8081/api/users").timeout(5000).build();

    Tenant tenant = Tenant.builder().id(tenantId).userProvider(userProvider).build();

    when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

    // When
    boolean result = adapter.validateScopes(clientId, tenantId, userId, scopes);

    // Then
    assertThat(result).isTrue();
    verify(restClient, never()).validateScopes(any(), any());
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

  // Helper method to set up client and tenant with MFA endpoint
  private void setupClientAndTenantWithMfaEndpoint(
      String clientId, String tenantId, String mfaEndpoint) {
    OAuthClient client = OAuthClient.builder().clientId(clientId).tenantId(tenantId).build();

    UserProvider userProvider =
        UserProvider.builder()
            .endpoint("http://localhost:8081/api/users")
            .mfaRegistrationEndpoint(mfaEndpoint.replace("user-123", "{userId}"))
            .timeout(5000)
            .build();

    Tenant tenant = Tenant.builder().id(tenantId).userProvider(userProvider).build();

    when(clientRepository.findByClientId(clientId)).thenReturn(Optional.of(client));
    when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
  }

  // Helper method to set up tenant directly with MFA endpoint
  private void setupTenantWithMfaEndpoint(String tenantId, String mfaEndpoint) {
    UserProvider userProvider =
        UserProvider.builder()
            .endpoint("http://localhost:8081/api/users")
            .mfaRegistrationEndpoint(mfaEndpoint.replace("user-123", "{userId}"))
            .timeout(5000)
            .build();

    Tenant tenant = Tenant.builder().id(tenantId).userProvider(userProvider).build();

    when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
  }

  // Helper method to set up tenant directly with scopes endpoint
  private void setupTenantWithScopesEndpoint(String tenantId, String scopesEndpoint) {
    UserProvider userProvider =
        UserProvider.builder()
            .endpoint("http://localhost:8081/api/users")
            .scopesValidationEndpoint(scopesEndpoint.replace("user-123", "{userId}"))
            .timeout(5000)
            .build();

    Tenant tenant = Tenant.builder().id(tenantId).userProvider(userProvider).build();

    when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
  }
}
