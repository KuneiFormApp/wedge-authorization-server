package com.kuneiform.infraestructure.config;

import com.kuneiform.infraestructure.config.properties.WedgeConfigProperties;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Flyway configuration for OAuth client database migrations. Only activated when database storage
 * is used. Handles schema creation if auto-create is enabled.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ClientDatabaseMigrationConfig {

  private final WedgeConfigProperties properties;

  @Bean
  @ConditionalOnProperty(name = "wedge.client-storage.type", havingValue = "postgresql")
  public Flyway clientDatabaseFlywayPostgres(DataSource clientDataSource) {
    return createFlywayBean(clientDataSource);
  }

  @Bean
  @ConditionalOnProperty(name = "wedge.client-storage.type", havingValue = "mysql")
  public Flyway clientDatabaseFlywayMysql(DataSource clientDataSource) {
    return createFlywayBean(clientDataSource);
  }

  @Bean
  @ConditionalOnProperty(name = "wedge.client-storage.type", havingValue = "sqlserver")
  public Flyway clientDatabaseFlywaySqlServer(DataSource clientDataSource) {
    return createFlywayBean(clientDataSource);
  }

  private Flyway createFlywayBean(DataSource clientDataSource) {
    String storageType = properties.getClientStorage().getType();
    String location = "classpath:db/migration/" + storageType;

    log.info("Configuring Flyway migrations for {} client storage from {}", storageType, location);

    // Auto-create schema if enabled
    if (properties.getClientStorage().isAutoCreateSchema()) {
      createSchemaIfNotExists(clientDataSource);
    }

    Flyway flyway =
        Flyway.configure()
            .dataSource(clientDataSource)
            .locations(location)
            .baselineOnMigrate(true)
            .baselineVersion("0")
            .validateOnMigrate(true)
            .load();

    log.info("Starting Flyway migration for OAuth client database");
    flyway.migrate();
    log.info("Flyway migration completed successfully");

    return flyway;
  }

  /**
   * Creates the configured schema if it doesn't exist and auto-create is enabled. This is
   * database-specific logic.
   */
  private void createSchemaIfNotExists(DataSource dataSource) {
    String schemaName = properties.getClientStorage().getSchemaName();
    String storageType = properties.getClientStorage().getType();

    if (schemaName == null || schemaName.isEmpty() || "public".equalsIgnoreCase(schemaName)) {
      log.debug("Using default schema, skipping schema creation");
      return;
    }

    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
    String createSchemaSql;

    try {
      switch (storageType.toLowerCase()) {
        case "postgresql":
          createSchemaSql = "CREATE SCHEMA IF NOT EXISTS " + schemaName;
          break;
        case "mysql":
          createSchemaSql = "CREATE SCHEMA IF NOT EXISTS " + schemaName;
          break;
        case "sqlserver":
          // SQL Server uses a different approach - check if schema exists first
          String checkSql =
              "IF NOT EXISTS (SELECT * FROM sys.schemas WHERE name = ?) EXEC('CREATE SCHEMA "
                  + schemaName
                  + "')";
          jdbcTemplate.execute(checkSql);
          log.info("Schema '{}' created or verified for SQL Server", schemaName);
          return;
        default:
          log.warn("Unknown storage type {}, skipping schema creation", storageType);
          return;
      }

      jdbcTemplate.execute(createSchemaSql);
      log.info("Schema '{}' created or verified for {}", schemaName, storageType);
    } catch (Exception e) {
      log.warn("Failed to create schema '{}' for {}: {}", schemaName, storageType, e.getMessage());
      // Don't fail startup if schema creation fails - Flyway might handle it
    }
  }
}
