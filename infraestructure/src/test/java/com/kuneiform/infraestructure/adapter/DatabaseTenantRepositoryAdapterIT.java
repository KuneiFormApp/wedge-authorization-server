package com.kuneiform.infraestructure.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import com.kuneiform.domain.model.Tenant;
import com.kuneiform.domain.model.UserProvider;
import com.kuneiform.infraestructure.config.ClientDatabaseMigrationConfig;
import com.kuneiform.infraestructure.config.ClientRepositoryConfig;
import com.kuneiform.infraestructure.config.properties.WedgeConfigProperties;
import com.kuneiform.infraestructure.persistence.entity.TenantEntity;
import com.kuneiform.infraestructure.persistence.repository.TenantJdbcRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
    classes = {
      DatabaseTenantRepositoryAdapterIT.TestConfiguration.class,
      ClientRepositoryConfig.class,
      ClientDatabaseMigrationConfig.class,
      WedgeConfigProperties.class
    })
@Testcontainers
@EnableJdbcRepositories(basePackages = "com.kuneiform.infraestructure.persistence.repository")
class DatabaseTenantRepositoryAdapterIT {

  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:15-alpine")
          .withDatabaseName("wedge_test")
          .withUsername("test")
          .withPassword("test");

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    registry.add("wedge.client-storage.type", () -> "postgresql");
    registry.add("wedge.client-storage.url", postgres::getJdbcUrl);
    registry.add("wedge.client-storage.username", postgres::getUsername);
    registry.add("wedge.client-storage.password", postgres::getPassword);
    registry.add("wedge.client-storage.driver-class-name", () -> "org.postgresql.Driver");
  }

  @Configuration
  @EnableAutoConfiguration
  static class TestConfiguration {
    @Bean
    public PasswordEncoder passwordEncoder() {
      return new BCryptPasswordEncoder();
    }
  }

  @Autowired private TenantJdbcRepository repository;
  @Autowired private JdbcTemplate jdbcTemplate;

  private DatabaseTenantRepositoryAdapter adapter;

  @BeforeEach
  void setUp() {
    // Create tenants table
    jdbcTemplate.execute(
        """
      CREATE TABLE IF NOT EXISTS tenants (
        id VARCHAR(255) PRIMARY KEY,
        name VARCHAR(255) NOT NULL,
        user_provider_endpoint VARCHAR(500) NOT NULL,
        user_provider_timeout INT NOT NULL DEFAULT 5000,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      )
      """);

    // Insert test tenants
    TenantEntity tenant1 = new TenantEntity();
    tenant1.setId("tenant-1");
    tenant1.setName("Tenant One");
    tenant1.setUserProviderEndpoint("http://localhost:8081/api/users/validate");
    tenant1.setUserProviderTimeout(5000);

    TenantEntity tenant2 = new TenantEntity();
    tenant2.setId("tenant-2");
    tenant2.setName("Tenant Two");
    tenant2.setUserProviderEndpoint("http://localhost:8082/api/users/validate");
    tenant2.setUserProviderTimeout(3000);

    repository.save(tenant1);
    repository.save(tenant2);

    adapter = new DatabaseTenantRepositoryAdapter(repository);
  }

  @Test
  void shouldLoadTenantsFromDatabase() {
    // When
    List<Tenant> tenants = adapter.findAll();

    // Then
    assertThat(tenants).hasSize(2);
  }

  @Test
  void shouldFindTenantById() {
    // When
    Optional<Tenant> tenant = adapter.findById("tenant-1");

    // Then
    assertTrue(tenant.isPresent());
    assertEquals("tenant-1", tenant.get().getId());
    assertEquals("Tenant One", tenant.get().getName());

    UserProvider userProvider = tenant.get().getUserProvider();
    assertNotNull(userProvider);
    assertEquals("http://localhost:8081/api/users/validate", userProvider.getEndpoint());
    assertEquals(5000, userProvider.getTimeout());
  }

  @Test
  void shouldReturnEmptyWhenTenantNotFound() {
    // When
    Optional<Tenant> tenant = adapter.findById("nonexistent");

    // Then
    assertTrue(tenant.isEmpty());
  }

  @Test
  void shouldCacheTenants() {
    // Given - first call loads from database
    Optional<Tenant> firstCall = adapter.findById("tenant-1");

    // When - second call should use cache
    Optional<Tenant> secondCall = adapter.findById("tenant-1");

    // Then
    assertTrue(firstCall.isPresent());
    assertTrue(secondCall.isPresent());
    // Both should be the same instance from cache
    assertThat(firstCall.get()).isSameAs(secondCall.get());
  }

  @Test
  void shouldHandleNullTimeout() {
    // Given
    TenantEntity tenant3 = new TenantEntity();
    tenant3.setId("tenant-3");
    tenant3.setName("Tenant Three");
    tenant3.setUserProviderEndpoint("http://localhost:8083/api/users/validate");
    tenant3.setUserProviderTimeout(null); // Null timeout
    repository.save(tenant3);

    // Create new adapter to reload cache
    DatabaseTenantRepositoryAdapter newAdapter = new DatabaseTenantRepositoryAdapter(repository);

    // When
    Optional<Tenant> tenant = newAdapter.findById("tenant-3");

    // Then
    assertTrue(tenant.isPresent());
    assertEquals(5000, tenant.get().getUserProvider().getTimeout()); // Default value
  }
}
