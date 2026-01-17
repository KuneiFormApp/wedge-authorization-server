package com.kuneiform.infraestructure.adapter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.kuneiform.domain.model.OAuthClient;
import com.kuneiform.infraestructure.config.properties.WedgeConfigProperties;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class YamlClientRepositoryAdapterTest {

  @Mock private PasswordEncoder passwordEncoder;
  @Mock private com.kuneiform.domain.port.TenantClientPort tenantClientPort;

  private YamlClientRepositoryAdapter adapter;

  @BeforeEach
  void setUp() {
    WedgeConfigProperties config = new WedgeConfigProperties();

    // Create a test client with null collections
    WedgeConfigProperties.ClientConfig clientConfig = new WedgeConfigProperties.ClientConfig();
    clientConfig.setClientId("test-client");
    clientConfig.setClientName("Test Client");
    clientConfig.setClientSecret(null); // Public client
    clientConfig.setClientAuthenticationMethods(List.of("none"));
    clientConfig.setAuthorizationGrantTypes(List.of("authorization_code"));
    clientConfig.setRedirectUris(List.of("http://localhost:3000/callback"));
    clientConfig.setPostLogoutRedirectUris(null); // This can be null
    clientConfig.setScopes(List.of("openid", "profile"));
    clientConfig.setRequireAuthorizationConsent(false);
    clientConfig.setRequirePkce(true);
    clientConfig.setTenantId("test-tenant");

    config.setClients(List.of(clientConfig));

    adapter = new YamlClientRepositoryAdapter(config, passwordEncoder, tenantClientPort);
  }

  @Test
  void shouldLoadClientWithNullPostLogoutRedirectUris() {
    // When
    Optional<OAuthClient> client = adapter.findByClientId("test-client");

    // Then
    assertTrue(client.isPresent());
    assertEquals("test-client", client.get().getClientId());
    assertEquals("Test Client", client.get().getClientName());
    assertNotNull(client.get().getPostLogoutRedirectUris());
    assertTrue(
        client.get().getPostLogoutRedirectUris().isEmpty(),
        "Post logout redirect URIs should be empty set when null in config");
  }

  @Test
  void shouldFindClientByClientId() {
    // When
    Optional<OAuthClient> client = adapter.findByClientId("test-client");

    // Then
    assertTrue(client.isPresent());
    assertEquals("test-client", client.get().getClientId());
  }

  @Test
  void shouldReturnEmptyWhenClientNotFound() {
    // When
    Optional<OAuthClient> client = adapter.findByClientId("nonexistent");

    // Then
    assertTrue(client.isEmpty());
  }

  @Test
  void shouldValidatePublicClientWithoutSecret() {
    // When
    boolean isValid = adapter.validateClient("test-client", null);

    // Then
    assertTrue(isValid, "Public client should be valid without secret");
  }

  @Test
  void shouldValidateConfidentialClient() {
    // Given
    WedgeConfigProperties config = new WedgeConfigProperties();
    WedgeConfigProperties.ClientConfig clientConfig = new WedgeConfigProperties.ClientConfig();
    clientConfig.setClientId("confidential-client");
    clientConfig.setClientName("Confidential Client");
    clientConfig.setClientSecret("secret123");
    clientConfig.setClientAuthenticationMethods(List.of("client_secret_basic"));
    clientConfig.setAuthorizationGrantTypes(List.of("client_credentials"));
    clientConfig.setRedirectUris(List.of("http://localhost:8080/callback"));
    clientConfig.setScopes(List.of("read", "write"));
    clientConfig.setTenantId("test-tenant");

    config.setClients(List.of(clientConfig));

    when(passwordEncoder.encode("secret123")).thenReturn("$encoded$secret123");
    when(passwordEncoder.matches("secret123", "$encoded$secret123")).thenReturn(true);

    YamlClientRepositoryAdapter confidentialAdapter =
        new YamlClientRepositoryAdapter(config, passwordEncoder, tenantClientPort);

    // When
    boolean isValid = confidentialAdapter.validateClient("confidential-client", "secret123");

    // Then
    assertTrue(isValid, "Confidential client should be valid with correct secret");
    verify(passwordEncoder).matches("secret123", "$encoded$secret123");
  }

  @Test
  void shouldRejectConfidentialClientWithWrongSecret() {
    // Given
    WedgeConfigProperties config = new WedgeConfigProperties();
    WedgeConfigProperties.ClientConfig clientConfig = new WedgeConfigProperties.ClientConfig();
    clientConfig.setClientId("confidential-client");
    clientConfig.setClientSecret("secret123");
    clientConfig.setClientName("Confidential Client");
    clientConfig.setClientAuthenticationMethods(List.of("client_secret_basic"));
    clientConfig.setAuthorizationGrantTypes(List.of("client_credentials"));
    clientConfig.setRedirectUris(List.of("http://localhost:8080/callback"));
    clientConfig.setScopes(List.of("read"));
    clientConfig.setTenantId("test-tenant");

    config.setClients(List.of(clientConfig));

    when(passwordEncoder.encode("secret123")).thenReturn("$encoded$secret123");
    when(passwordEncoder.matches("wrong-secret", "$encoded$secret123")).thenReturn(false);

    YamlClientRepositoryAdapter confidentialAdapter =
        new YamlClientRepositoryAdapter(config, passwordEncoder, tenantClientPort);

    // When
    boolean isValid = confidentialAdapter.validateClient("confidential-client", "wrong-secret");

    // Then
    assertFalse(isValid, "Confidential client should be invalid with wrong secret");
  }

  @Test
  void shouldHandleNullCollectionsInConfig() {
    // Given
    WedgeConfigProperties config = new WedgeConfigProperties();
    WedgeConfigProperties.ClientConfig clientConfig = new WedgeConfigProperties.ClientConfig();
    clientConfig.setClientId("minimal-client");
    clientConfig.setClientName("Minimal Client");
    // All collections are null
    clientConfig.setClientAuthenticationMethods(null);
    clientConfig.setAuthorizationGrantTypes(null);
    clientConfig.setRedirectUris(null);
    clientConfig.setPostLogoutRedirectUris(null);
    clientConfig.setScopes(null);
    clientConfig.setTenantId("test-tenant");

    config.setClients(List.of(clientConfig));

    // When
    YamlClientRepositoryAdapter minimalAdapter =
        new YamlClientRepositoryAdapter(config, passwordEncoder, tenantClientPort);
    Optional<OAuthClient> client = minimalAdapter.findByClientId("minimal-client");

    // Then
    assertTrue(client.isPresent());
    assertNotNull(client.get().getClientAuthenticationMethods());
    assertNotNull(client.get().getAuthorizationGrantTypes());
    assertNotNull(client.get().getRedirectUris());
    assertNotNull(client.get().getPostLogoutRedirectUris());
    assertNotNull(client.get().getScopes());
    assertTrue(client.get().getClientAuthenticationMethods().isEmpty());
    assertTrue(client.get().getAuthorizationGrantTypes().isEmpty());
    assertTrue(client.get().getRedirectUris().isEmpty());
    assertTrue(client.get().getPostLogoutRedirectUris().isEmpty());
    assertTrue(client.get().getScopes().isEmpty());
  }

  @Test
  void shouldNotEncodeAlreadyHashedSecretWith2a() {
    // Given: A pre-hashed bcrypt secret (most common format)
    String preHashedSecret = "$2a$10$N9qo8uLOickgx2ZMRZoMye1p0Z6QjLk8cZJ6XXXXXXXXXXXXX";
    WedgeConfigProperties config = new WedgeConfigProperties();
    WedgeConfigProperties.ClientConfig clientConfig = new WedgeConfigProperties.ClientConfig();
    clientConfig.setClientId("hashed-client");
    clientConfig.setClientName("Pre-hashed Client");
    clientConfig.setClientSecret(preHashedSecret);
    clientConfig.setClientAuthenticationMethods(List.of("client_secret_basic"));
    clientConfig.setAuthorizationGrantTypes(List.of("client_credentials"));
    clientConfig.setRedirectUris(List.of("http://localhost:8080/callback"));
    clientConfig.setScopes(List.of("read"));
    clientConfig.setTenantId("test-tenant");

    config.setClients(List.of(clientConfig));

    // passwordEncoder.encode() should NOT be called for pre-hashed secrets
    when(passwordEncoder.matches("plaintext-password", preHashedSecret)).thenReturn(true);

    YamlClientRepositoryAdapter hashedAdapter =
        new YamlClientRepositoryAdapter(config, passwordEncoder, tenantClientPort);

    // Then: The secret should be stored as-is without re-encoding
    Optional<OAuthClient> client = hashedAdapter.findByClientId("hashed-client");
    assertTrue(client.isPresent());
    assertEquals(preHashedSecret, client.get().getClientSecret());

    // Verify encode was never called (because secret was already hashed)
    verify(passwordEncoder, never()).encode(anyString());

    // Validation should work with the pre-hashed secret
    boolean isValid = hashedAdapter.validateClient("hashed-client", "plaintext-password");
    assertTrue(isValid);
    verify(passwordEncoder).matches("plaintext-password", preHashedSecret);
  }

  @Test
  void shouldNotEncodeAlreadyHashedSecretWith2b() {
    // Given: bcrypt hash with $2b$ variant
    String preHashedSecret = "$2b$10$XYZ1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567";
    WedgeConfigProperties config = new WedgeConfigProperties();
    WedgeConfigProperties.ClientConfig clientConfig = new WedgeConfigProperties.ClientConfig();
    clientConfig.setClientId("hashed-client-2b");
    clientConfig.setClientSecret(preHashedSecret);
    clientConfig.setClientName("Hashed Client 2b");
    clientConfig.setClientAuthenticationMethods(List.of("client_secret_post"));
    clientConfig.setAuthorizationGrantTypes(List.of("authorization_code"));
    clientConfig.setRedirectUris(List.of("http://localhost:3000/callback"));
    clientConfig.setScopes(List.of("openid"));
    clientConfig.setTenantId("test-tenant");

    config.setClients(List.of(clientConfig));

    YamlClientRepositoryAdapter hashedAdapter =
        new YamlClientRepositoryAdapter(config, passwordEncoder, tenantClientPort);

    // Then: The secret should be stored as-is
    Optional<OAuthClient> client = hashedAdapter.findByClientId("hashed-client-2b");
    assertTrue(client.isPresent());
    assertEquals(preHashedSecret, client.get().getClientSecret());
    verify(passwordEncoder, never()).encode(anyString());
  }

  @Test
  void shouldNotEncodeAlreadyHashedSecretWith2y() {
    // Given: bcrypt hash with $2y$ variant
    String preHashedSecret = "$2y$12$ABC9876543210ZYXWVUTSRQPONMLKJIHGFEDCBA98765";
    WedgeConfigProperties config = new WedgeConfigProperties();
    WedgeConfigProperties.ClientConfig clientConfig = new WedgeConfigProperties.ClientConfig();
    clientConfig.setClientId("hashed-client-2y");
    clientConfig.setClientSecret(preHashedSecret);
    clientConfig.setClientName("Hashed Client 2y");
    clientConfig.setClientAuthenticationMethods(List.of("client_secret_basic"));
    clientConfig.setAuthorizationGrantTypes(List.of("client_credentials"));
    clientConfig.setRedirectUris(List.of("http://localhost:8080"));
    clientConfig.setScopes(List.of("admin"));
    clientConfig.setTenantId("test-tenant");

    config.setClients(List.of(clientConfig));

    YamlClientRepositoryAdapter hashedAdapter =
        new YamlClientRepositoryAdapter(config, passwordEncoder, tenantClientPort);

    // Then: The secret should be stored as-is
    Optional<OAuthClient> client = hashedAdapter.findByClientId("hashed-client-2y");
    assertTrue(client.isPresent());
    assertEquals(preHashedSecret, client.get().getClientSecret());
    verify(passwordEncoder, never()).encode(anyString());
  }

  @Test
  void shouldEncodePlainTextSecrets() {
    // Given: A plain text secret (not a bcrypt hash)
    WedgeConfigProperties config = new WedgeConfigProperties();
    WedgeConfigProperties.ClientConfig clientConfig = new WedgeConfigProperties.ClientConfig();
    clientConfig.setClientId("plaintext-client");
    clientConfig.setClientName("Plaintext Client");
    clientConfig.setClientSecret("my-plain-secret");
    clientConfig.setClientAuthenticationMethods(List.of("client_secret_basic"));
    clientConfig.setAuthorizationGrantTypes(List.of("authorization_code"));
    clientConfig.setRedirectUris(List.of("http://localhost:3000/callback"));
    clientConfig.setScopes(List.of("read", "write"));
    clientConfig.setTenantId("test-tenant");

    config.setClients(List.of(clientConfig));

    when(passwordEncoder.encode("my-plain-secret")).thenReturn("$2a$10$ENCODED_VERSION");

    YamlClientRepositoryAdapter plaintextAdapter =
        new YamlClientRepositoryAdapter(config, passwordEncoder, tenantClientPort);

    // Then: The plain text secret should be encoded
    Optional<OAuthClient> client = plaintextAdapter.findByClientId("plaintext-client");
    assertTrue(client.isPresent());
    assertEquals("$2a$10$ENCODED_VERSION", client.get().getClientSecret());
    verify(passwordEncoder).encode("my-plain-secret");
  }
}
