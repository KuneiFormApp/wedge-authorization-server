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
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(classes = {
    DatabaseTenantRepositoryAdapterMysqlIT.TestConfiguration.class,
    ClientRepositoryConfig.class,
    ClientDatabaseMigrationConfig.class,
    WedgeConfigProperties.class
})
@Testcontainers
class DatabaseTenantRepositoryAdapterMysqlIT {

  @Container
  static MySQLContainer<?> mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.0.44-oracle"))
      .withDatabaseName("wedge_test")
      .withUsername("test")
      .withPassword("test");

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", mysql::getJdbcUrl);
    registry.add("spring.datasource.username", mysql::getUsername);
    registry.add("spring.datasource.password", mysql::getPassword);
    registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
    registry.add("wedge.client-storage.type", () -> "mysql");
    registry.add("wedge.client-storage.url", mysql::getJdbcUrl);
    registry.add("wedge.client-storage.username", mysql::getUsername);
    registry.add("wedge.client-storage.password", mysql::getPassword);
    registry.add("wedge.client-storage.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
  }

  @Configuration
  @EnableAutoConfiguration
  static class TestConfiguration {
    @Bean
    public PasswordEncoder passwordEncoder() {
      return new BCryptPasswordEncoder();
    }
  }

  @Autowired
  private TenantJdbcRepository repository;
  @Autowired
  private JdbcTemplate jdbcTemplate;

  private DatabaseTenantRepositoryAdapter adapter;

  @BeforeEach
  void setUp() {
    // Clean up existing data
    repository.deleteAll();

    // MySQL table creation is handled by Flyway migration in
    // ClientDatabaseMigrationConfig
    // But we need to ensure the schema matches what the test expects or relies on
    // Flyway
    // Since we are running with SpringBootTest and dynamic properties for MySQL,
    // ClientDatabaseMigrationConfig should pick up the mysql migration.
    // However, DatabaseTenantRepositoryAdapterIT manually created the table.
    // Let's rely on Flyway for MySQL as well to be safer, or replicate the manual
    // creation if
    // needed.
    // The previous test manually created the table because it might run before
    // migrations or for
    // simplicity.
    // But verify if we should just let Flyway do it.
    // Given the production complexity with @DependsOn, best to use the real
    // mechanism.
    // But to match the previous test style, I'll add manual creation as
    // fallback/check.

    // Actually, for MySQL, let's try to let Flyway handle it as configured in the
    // app context.
    // But wait, the original test manually created the table. Let's do the same but
    // with MySQL
    // syntax.

    // Create tenants table if not exists (MySQL syntax is same for simplified
    // table)
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
