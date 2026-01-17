package com.kuneiform.infraestructure.adapter;

import static org.junit.jupiter.api.Assertions.*;

import com.kuneiform.domain.model.Tenant;
import com.kuneiform.domain.model.UserProvider;
import com.kuneiform.infraestructure.config.properties.WedgeConfigProperties;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class YamlTenantRepositoryAdapterTest {

  private YamlTenantRepositoryAdapter adapter;

  @BeforeEach
  void setUp() {
    WedgeConfigProperties config = new WedgeConfigProperties();

    // Create test tenants
    WedgeConfigProperties.TenantConfig tenant1 = new WedgeConfigProperties.TenantConfig();
    tenant1.setId("tenant-1");
    tenant1.setName("Tenant One");
    WedgeConfigProperties.UserProviderConfig userProvider1 =
        new WedgeConfigProperties.UserProviderConfig();
    userProvider1.setEndpoint("http://localhost:8081/api/users/validate");
    userProvider1.setTimeout(5000);
    tenant1.setUserProvider(userProvider1);

    WedgeConfigProperties.TenantConfig tenant2 = new WedgeConfigProperties.TenantConfig();
    tenant2.setId("tenant-2");
    tenant2.setName("Tenant Two");
    WedgeConfigProperties.UserProviderConfig userProvider2 =
        new WedgeConfigProperties.UserProviderConfig();
    userProvider2.setEndpoint("http://localhost:8082/api/users/validate");
    userProvider2.setTimeout(3000);
    tenant2.setUserProvider(userProvider2);

    config.setTenants(List.of(tenant1, tenant2));

    adapter = new YamlTenantRepositoryAdapter(config);
  }

  @Test
  void shouldLoadTenantsFromConfig() {
    // When
    List<Tenant> tenants = adapter.findAll();

    // Then
    assertEquals(2, tenants.size());
  }

  @Test
  void shouldFindTenantById() {
    // When
    Optional<Tenant> tenant = adapter.findById("tenant-1");

    // Then
    assertTrue(tenant.isPresent());
    assertEquals("tenant-1", tenant.get().getId());
    assertEquals("Tenant One", tenant.get().getName());
    assertNotNull(tenant.get().getUserProvider());
    assertEquals(
        "http://localhost:8081/api/users/validate", tenant.get().getUserProvider().getEndpoint());
    assertEquals(5000, tenant.get().getUserProvider().getTimeout());
  }

  @Test
  void shouldReturnEmptyWhenTenantNotFound() {
    // When
    Optional<Tenant> tenant = adapter.findById("nonexistent");

    // Then
    assertTrue(tenant.isEmpty());
  }

  @Test
  void shouldMapUserProviderCorrectly() {
    // When
    Optional<Tenant> tenant = adapter.findById("tenant-2");

    // Then
    assertTrue(tenant.isPresent());
    UserProvider userProvider = tenant.get().getUserProvider();
    assertNotNull(userProvider);
    assertEquals("http://localhost:8082/api/users/validate", userProvider.getEndpoint());
    assertEquals(3000, userProvider.getTimeout());
  }

  @Test
  void shouldHandleEmptyTenantsList() {
    // Given
    WedgeConfigProperties emptyConfig = new WedgeConfigProperties();
    emptyConfig.setTenants(List.of());

    // When
    YamlTenantRepositoryAdapter emptyAdapter = new YamlTenantRepositoryAdapter(emptyConfig);
    List<Tenant> tenants = emptyAdapter.findAll();

    // Then
    assertNotNull(tenants);
    assertTrue(tenants.isEmpty());
  }
}
