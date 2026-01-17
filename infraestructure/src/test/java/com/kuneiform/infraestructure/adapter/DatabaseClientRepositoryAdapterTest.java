package com.kuneiform.infraestructure.adapter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.kuneiform.domain.model.OAuthClient;
import com.kuneiform.domain.port.TenantClientPort;
import com.kuneiform.infraestructure.config.properties.WedgeConfigProperties;
import com.kuneiform.infraestructure.persistence.entity.OAuthClientEntity;
import com.kuneiform.infraestructure.persistence.repository.OAuthClientJdbcRepository;
import com.kuneiform.infraestructure.security.TenantContext;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class DatabaseClientRepositoryAdapterTest {

  @Mock private OAuthClientJdbcRepository repository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private TenantClientPort tenantClientPort;
  @Mock private WedgeConfigProperties properties;
  @Mock private WedgeConfigProperties.MultiTenancyConfig multiTenancyConfig;

  private DatabaseClientRepositoryAdapter adapter;
  private MockedStatic<TenantContext> tenantContextMock;

  @BeforeEach
  void setUp() {
    // Setup default properties
    when(properties.getMultiTenancy()).thenReturn(multiTenancyConfig);
    when(multiTenancyConfig.isEnabled()).thenReturn(false); // Disabled by default for simpler tests

    // Initial cache loading requires a repository call
    when(repository.findAll()).thenReturn(Collections.emptyList());

    tenantContextMock = mockStatic(TenantContext.class);

    adapter =
        new DatabaseClientRepositoryAdapter(
            repository, passwordEncoder, tenantClientPort, properties);
  }

  @AfterEach
  void tearDown() {
    tenantContextMock.close();
  }

  @Test
  void shouldFindClientByIdFromCache() {
    // Re-initialize adapter with a client in DB to populate cache
    OAuthClientEntity entity = createEntity("client-1", "secret");
    when(repository.findAll()).thenReturn(Collections.singletonList(entity));

    adapter =
        new DatabaseClientRepositoryAdapter(
            repository, passwordEncoder, tenantClientPort, properties);

    // When
    Optional<OAuthClient> result = adapter.findByClientId("client-1");

    // Then
    assertTrue(result.isPresent());
    assertEquals("client-1", result.get().getClientId());
    // Should NOT call repository.findByClientId since it's in cache
    verify(repository, never()).findByClientId("client-1");
  }

  @Test
  void shouldFindClientByIdFromDatabaseWhenNotCached() {
    // Given - Cache empty initially
    OAuthClientEntity entity = createEntity("client-db", "secret");
    when(repository.findByClientId("client-db")).thenReturn(Optional.of(entity));

    // When
    Optional<OAuthClient> result = adapter.findByClientId("client-db");

    // Then
    assertTrue(result.isPresent());
    assertEquals("client-db", result.get().getClientId());
    verify(repository).findByClientId("client-db");
  }

  @Test
  void shouldReturnEmptyWhenClientNotFound() {
    // Given
    when(repository.findByClientId("unknown")).thenReturn(Optional.empty());

    // When
    Optional<OAuthClient> result = adapter.findByClientId("unknown");

    // Then
    assertTrue(result.isEmpty());
  }

  @Test
  void shouldValidateConfidentialClientWithCorrectSecret() {
    // Given
    OAuthClientEntity entity = createEntity("confidential-client", "encoded-secret");
    when(repository.findByClientId("confidential-client")).thenReturn(Optional.of(entity));
    when(passwordEncoder.matches("raw-secret", "encoded-secret")).thenReturn(true);

    // When
    boolean isValid = adapter.validateClient("confidential-client", "raw-secret");

    // Then
    assertTrue(isValid);
  }

  @Test
  void shouldRejectConfidentialClientWithWrongSecret() {
    // Given
    OAuthClientEntity entity = createEntity("confidential-client", "encoded-secret");
    when(repository.findByClientId("confidential-client")).thenReturn(Optional.of(entity));
    when(passwordEncoder.matches("wrong-secret", "encoded-secret")).thenReturn(false);

    // When
    boolean isValid = adapter.validateClient("confidential-client", "wrong-secret");

    // Then
    assertFalse(isValid);
  }

  @Test
  void shouldValidatePublicClientWithoutSecret() {
    // Given
    OAuthClientEntity entity = createEntity("public-client", null);
    // Mimic public client behavior (no secret needed)
    // Note: DatabaseClientRepositoryAdapter.toOAuthClient logic determines isPublic based on
    // authentication methods usually, or if secret is empty.
    // Let's assume the entity conversion handles it or we mock internal logic?
    // Looking at the adapter code, it assumes validation is true if client.isPublic().
    // We need to ensure toOAuthClient maps it correctly.
    // But since toOAuthClient is private, we depend on its logic.
    // Let's construct an entity that results in public client.
    // If clientSecret is null/empty, typically public.

    // Actually, looking at the code:
    // public boolean isPublic() {
    //    return clientAuthenticationMethods != null
    //        && clientAuthenticationMethods.contains(ClientAuthenticationMethod.NONE.getValue());
    // }
    // Wait, OAuthClient model defines isPublic logic. I assume passing "none" in auth methods.

    entity.setClientAuthenticationMethods("none");

    when(repository.findByClientId("public-client")).thenReturn(Optional.of(entity));

    // When
    boolean isValid = adapter.validateClient("public-client", null);

    // Then
    assertTrue(isValid);
  }

  @Test
  void shouldSaveClientAndUpdateCache() {
    // Given
    OAuthClient client =
        OAuthClient.builder().clientId("new-client").clientSecret("raw-secret").build();

    OAuthClientEntity savedEntity = createEntity("new-client", "encoded-secret");

    when(passwordEncoder.encode("raw-secret")).thenReturn("encoded-secret");
    when(repository.save(any(OAuthClientEntity.class))).thenReturn(savedEntity);

    // When
    OAuthClient savedClient = adapter.save(client);

    // Then
    assertEquals("new-client", savedClient.getClientId());
    assertEquals("encoded-secret", savedClient.getClientSecret());

    // Verify it's now in cache (should return without DB call)
    // We can't easily peek into private cache, but if we mock repository to return empty next time,
    // and findByClientId returns it, then it's cached.
    reset(repository);
    Optional<OAuthClient> cached = adapter.findByClientId("new-client");
    assertTrue(cached.isPresent());
    verify(repository, never()).findByClientId("new-client");
  }

  @Test
  void shouldDeleteClientAndRemoveFromCache() {
    // Given
    // Pre-populate cache
    OAuthClientEntity entity = createEntity("client-to-delete", "secret");
    when(repository.findAll()).thenReturn(Collections.singletonList(entity));

    // Re-init to load cache
    adapter =
        new DatabaseClientRepositoryAdapter(
            repository, passwordEncoder, tenantClientPort, properties);

    // When
    adapter.deleteByClientId("client-to-delete");

    // Then
    verify(repository).deleteByClientId("client-to-delete");

    // Verify removed from cache (should fallback to DB, mock DB empty)
    when(repository.findByClientId("client-to-delete")).thenReturn(Optional.empty());
    Optional<OAuthClient> result = adapter.findByClientId("client-to-delete");
    assertTrue(result.isEmpty());
  }

  @Test
  void shouldFilterClientByTenantWhenMultiTenancyEnabled() {
    // Setup Multi-tenancy
    when(multiTenancyConfig.isEnabled()).thenReturn(true);
    // Re-init adapter
    when(repository.findAll()).thenReturn(Collections.emptyList());
    adapter =
        new DatabaseClientRepositoryAdapter(
            repository, passwordEncoder, tenantClientPort, properties);

    // Setup Context
    tenantContextMock.when(TenantContext::hasTenant).thenReturn(true);
    tenantContextMock.when(TenantContext::getCurrentTenant).thenReturn("tenant-1");

    // Client exists in DB
    OAuthClientEntity entity = createEntity("client-1", "secret");
    when(repository.findByClientId("client-1")).thenReturn(Optional.of(entity));

    // Case 1: Client allowed in tenant
    when(tenantClientPort.isClientInTenant("tenant-1", "client-1")).thenReturn(true);
    Optional<OAuthClient> allowed = adapter.findByClientId("client-1");
    assertTrue(allowed.isPresent());

    // Case 2: Client NOT allowed in tenant
    when(tenantClientPort.isClientInTenant("tenant-1", "client-1")).thenReturn(false);
    Optional<OAuthClient> denied = adapter.findByClientId("client-1");
    assertTrue(denied.isEmpty());
  }

  private OAuthClientEntity createEntity(String clientId, String clientSecret) {
    OAuthClientEntity entity = new OAuthClientEntity();
    entity.setId(1L);
    entity.setClientId(clientId);
    entity.setClientSecret(clientSecret);
    entity.setClientAuthenticationMethods("client_secret_basic");
    entity.setAuthorizationGrantTypes("authorization_code");
    entity.setRedirectUris("http://localhost/cb");
    entity.setScopes("openid");
    return entity;
  }
}
