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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(
    classes = {
      DatabaseTenantRepositoryAdapterSqlServerIT.TestConfiguration.class,
      ClientRepositoryConfig.class,
      ClientDatabaseMigrationConfig.class,
      WedgeConfigProperties.class
    })
@Testcontainers
class DatabaseTenantRepositoryAdapterSqlServerIT {

  @Container
  static MSSQLServerContainer<?> sqlserver =
      new MSSQLServerContainer<>(
              DockerImageName.parse("mcr.microsoft.com/mssql/server:2022-latest"))
          .acceptLicense();

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", sqlserver::getJdbcUrl);
    registry.add("spring.datasource.username", sqlserver::getUsername);
    registry.add("spring.datasource.password", sqlserver::getPassword);
    registry.add(
        "spring.datasource.driver-class-name",
        () -> "com.microsoft.sqlserver.jdbc.SQLServerDriver");
    registry.add("wedge.client-storage.type", () -> "sqlserver");
    registry.add("wedge.client-storage.url", sqlserver::getJdbcUrl);
    registry.add("wedge.client-storage.username", sqlserver::getUsername);
    registry.add("wedge.client-storage.password", sqlserver::getPassword);
    registry.add(
        "wedge.client-storage.driver-class-name",
        () -> "com.microsoft.sqlserver.jdbc.SQLServerDriver");
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
    // Clean up existing data
    repository.deleteAll();

    // Create tenants table manually for test setup if not present
    // SQL Server syntax for checking table existence
    jdbcTemplate.execute(
        """
            IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'tenants')
            BEGIN
              CREATE TABLE tenants (
                id NVARCHAR(255) PRIMARY KEY,
                name NVARCHAR(255) NOT NULL,
                user_provider_endpoint NVARCHAR(500) NOT NULL,
                user_provider_timeout INT NOT NULL DEFAULT 5000,
                created_at DATETIME2 DEFAULT GETDATE(),
                updated_at DATETIME2 DEFAULT GETDATE()
              )
            END
            """);

    // Insert test tenants
    TenantEntity tenant1 = new TenantEntity();
    tenant1.setId("tenant-1");
    tenant1.setName("Tenant One");
    tenant1.setUserProviderEndpoint("http://localhost:8081/api/users/validate");
    tenant1.setUserProviderTimeout(5000);
    tenant1.setIsNew(true);

    TenantEntity tenant2 = new TenantEntity();
    tenant2.setId("tenant-2");
    tenant2.setName("Tenant Two");
    tenant2.setUserProviderEndpoint("http://localhost:8082/api/users/validate");
    tenant2.setUserProviderTimeout(3000);
    tenant2.setIsNew(true);

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
}
