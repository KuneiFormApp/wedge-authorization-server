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
class StaticClientRepositoryAdapterTest {

  @Mock private PasswordEncoder passwordEncoder;

  private StaticClientRepositoryAdapter adapter;

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

    config.setClients(List.of(clientConfig));

    adapter = new StaticClientRepositoryAdapter(config, passwordEncoder);
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

    config.setClients(List.of(clientConfig));

    when(passwordEncoder.encode("secret123")).thenReturn("$encoded$secret123");
    when(passwordEncoder.matches("secret123", "$encoded$secret123")).thenReturn(true);

    StaticClientRepositoryAdapter confidentialAdapter =
        new StaticClientRepositoryAdapter(config, passwordEncoder);

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

    config.setClients(List.of(clientConfig));

    when(passwordEncoder.encode("secret123")).thenReturn("$encoded$secret123");
    when(passwordEncoder.matches("wrong-secret", "$encoded$secret123")).thenReturn(false);

    StaticClientRepositoryAdapter confidentialAdapter =
        new StaticClientRepositoryAdapter(config, passwordEncoder);

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

    config.setClients(List.of(clientConfig));

    // When
    StaticClientRepositoryAdapter minimalAdapter =
        new StaticClientRepositoryAdapter(config, passwordEncoder);
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
}
