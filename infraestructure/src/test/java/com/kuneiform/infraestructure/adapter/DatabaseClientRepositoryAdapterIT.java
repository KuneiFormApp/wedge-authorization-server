package com.kuneiform.infraestructure.adapter;

import static org.junit.jupiter.api.Assertions.*;

import com.kuneiform.domain.model.OAuthClient;
import com.kuneiform.infraestructure.config.ClientDatabaseMigrationConfig;
import com.kuneiform.infraestructure.config.ClientRepositoryConfig;
import com.kuneiform.infraestructure.config.properties.WedgeConfigProperties;
import com.kuneiform.infraestructure.persistence.entity.TenantEntity;
import com.kuneiform.infraestructure.persistence.repository.OAuthClientJdbcRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration test for DatabaseClientRepositoryAdapter using Testcontainers with PostgreSQL. Tests
 * the full stack: database, Flyway migrations, Spring Data JDBC, and the repository adapter.
 */
@SpringBootTest(
    classes = {
      DatabaseClientRepositoryAdapterIT.TestConfiguration.class,
      ClientRepositoryConfig.class,
      ClientDatabaseMigrationConfig.class,
      WedgeConfigProperties.class
    })
@Testcontainers
@EnableJdbcRepositories(basePackages = "com.kuneiform.infraestructure.persistence.repository")
class DatabaseClientRepositoryAdapterIT {

  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("test_oauth_clients")
          .withUsername("test")
          .withPassword("test");

  @Autowired private DatabaseClientRepositoryAdapter adapter;

  @Autowired private OAuthClientJdbcRepository jdbcRepository;

  @Autowired
  private com.kuneiform.infraestructure.persistence.repository.TenantJdbcRepository
      tenantRepository;

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

  @BeforeEach
  void setUp() {
    // Clear database before each test
    jdbcRepository.deleteAll();
    tenantRepository.deleteAll();

    // Insert test tenant
    TenantEntity tenant = new TenantEntity();
    tenant.setId("test-tenant");
    tenant.setName("Test Tenant");
    tenant.setUserProviderEndpoint("http://localhost:8081/auth");
    tenant.setUserProviderTimeout(5000);
    tenant.setIsNew(true);

    tenantRepository.save(tenant);
  }

  @Test
  void shouldSaveAndRetrievePublicClient() {
    // Given
    OAuthClient client =
        OAuthClient.builder()
            .clientId("test-public-client")
            .clientName("Test Public Client")
            .clientSecret(null) // Public client
            .clientAuthenticationMethods(Set.of("none"))
            .authorizationGrantTypes(Set.of("authorization_code", "refresh_token"))
            .redirectUris(Set.of("http://localhost:3000/callback"))
            .postLogoutRedirectUris(Set.of("http://localhost:3000/"))
            .scopes(Set.of("openid", "profile", "email"))
            .requireAuthorizationConsent(false)
            .requirePkce(true)
            .tenantId("test-tenant")
            .build();

    // When
    OAuthClient saved = adapter.save(client);

    // Then
    assertNotNull(saved.getId());
    assertEquals("test-public-client", saved.getClientId());
    assertTrue(saved.isPublic());

    // Verify it can be retrieved
    Optional<OAuthClient> retrieved = adapter.findByClientId("test-public-client");
    assertTrue(retrieved.isPresent());
    assertEquals("Test Public Client", retrieved.get().getClientName());
    assertTrue(retrieved.get().getScopes().contains("openid"));
  }

  @Test
  void shouldSaveAndRetrieveConfidentialClient() {
    // Given
    OAuthClient client =
        OAuthClient.builder()
            .clientId("test-confidential-client")
            .clientName("Test Confidential Client")
            .clientSecret("my-secret") // Will be encoded
            .clientAuthenticationMethods(Set.of("client_secret_basic", "client_secret_post"))
            .authorizationGrantTypes(Set.of("client_credentials"))
            .redirectUris(Set.of("http://localhost:8082/callback"))
            .scopes(Set.of("read", "write", "admin"))
            .requireAuthorizationConsent(false)
            .requirePkce(true)
            .build();

    // When
    OAuthClient saved = adapter.save(client);

    // Then
    assertNotNull(saved.getId());
    assertNotNull(saved.getClientSecret());
    assertNotEquals("my-secret", saved.getClientSecret()); // Should be encoded
    assertTrue(saved.getClientSecret().startsWith("$2a$")); // BCrypt format

    // Verify validation works
    assertTrue(adapter.validateClient("test-confidential-client", "my-secret"));
    assertFalse(adapter.validateClient("test-confidential-client", "wrong-secret"));
  }

  @Test
  void shouldReturnAllClients() {
    // Given
    adapter.save(createTestClient("client-1", "Client 1"));
    adapter.save(createTestClient("client-2", "Client 2"));
    adapter.save(createTestClient("client-3", "Client 3"));

    // When
    List<OAuthClient> allClients = adapter.findAll();

    // Then
    assertEquals(3, allClients.size());
    assertTrue(allClients.stream().anyMatch(c -> c.getClientId().equals("client-1")));
    assertTrue(allClients.stream().anyMatch(c -> c.getClientId().equals("client-2")));
    assertTrue(allClients.stream().anyMatch(c -> c.getClientId().equals("client-3")));
  }

  @Test
  void shouldDeleteClient() {
    // Given
    adapter.save(createTestClient("client-to-delete", "Client to Delete"));
    assertTrue(adapter.findByClientId("client-to-delete").isPresent());

    // When
    adapter.deleteByClientId("client-to-delete");

    // Then
    assertFalse(adapter.findByClientId("client-to-delete").isPresent());
  }

  @Test
  void shouldHandleNullCollections() {
    // Given
    OAuthClient client =
        OAuthClient.builder()
            .clientId("minimal-client")
            .clientName("Minimal Client")
            .clientSecret(null)
            .clientAuthenticationMethods(new HashSet<>())
            .authorizationGrantTypes(new HashSet<>())
            .redirectUris(new HashSet<>())
            .postLogoutRedirectUris(new HashSet<>())
            .scopes(new HashSet<>())
            .requireAuthorizationConsent(false)
            .requirePkce(false)
            .tenantId("test-tenant")
            .build();

    // When
    OAuthClient saved = adapter.save(client);

    // Then
    assertNotNull(saved.getId());
    Optional<OAuthClient> retrieved = adapter.findByClientId("minimal-client");
    assertTrue(retrieved.isPresent());
    assertNotNull(retrieved.get().getClientAuthenticationMethods());
    assertTrue(retrieved.get().getClientAuthenticationMethods().isEmpty());
  }

  @Test
  void shouldReturnEmptyWhenClientNotFound() {
    // When
    Optional<OAuthClient> result = adapter.findByClientId("nonexistent-client");

    // Then
    assertTrue(result.isEmpty());
  }

  @Test
  void shouldValidatePublicClient() {
    // Given
    adapter.save(createTestClient("public-client", "Public Client"));

    // When/Then
    assertTrue(adapter.validateClient("public-client", null));
    assertTrue(adapter.validateClient("public-client", "any-value")); // Secret ignored for public
  }

  // Helper methods

  private OAuthClient createTestClient(String clientId, String clientName) {
    return OAuthClient.builder()
        .clientId(clientId)
        .clientName(clientName)
        .clientSecret(null)
        .clientAuthenticationMethods(Set.of("none"))
        .authorizationGrantTypes(Set.of("authorization_code"))
        .redirectUris(Set.of("http://localhost:3000/callback"))
        .scopes(Set.of("openid", "profile"))
        .requireAuthorizationConsent(false)
        .requirePkce(true)
        .tenantId("test-tenant")
        .build();
  }
}
