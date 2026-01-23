package com.kuneiform.infrastructure.adapter;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for integration tests that require a PostgreSQL database. Implements the Singleton
 * Container pattern to use a single container instance for all test classes extending this one,
 * optimizing resource usage involved in parallel execution.
 */
// @Testcontainers - Removed to avoid auto-management as we start it manually
public abstract class AbstractPostgresIntegrationTest {

  static final PostgreSQLContainer<?> postgres;

  static {
    postgres =
        new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("test_db")
            .withUsername("test")
            .withPassword("test");
    // Start manually to ensure singleton behavior across multiple test classes
    postgres.start();
  }

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    // Spring Datasource configuration
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

    // Wedge Client Storage configuration
    registry.add("wedge.client-storage.type", () -> "postgresql");
    registry.add("wedge.client-storage.url", postgres::getJdbcUrl);
    registry.add("wedge.client-storage.username", postgres::getUsername);
    registry.add("wedge.client-storage.password", postgres::getPassword);
    registry.add("wedge.client-storage.driver-class-name", () -> "org.postgresql.Driver");
  }
}
