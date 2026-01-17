package com.kuneiform.infraestructure.config;

import com.kuneiform.domain.port.ClientRepository;
import com.kuneiform.domain.port.TenantClientPort;
import com.kuneiform.domain.port.TenantRepository;
import com.kuneiform.infraestructure.adapter.DatabaseClientRepositoryAdapter;
import com.kuneiform.infraestructure.adapter.DatabaseTenantRepositoryAdapter;
import com.kuneiform.infraestructure.adapter.YamlClientRepositoryAdapter;
import com.kuneiform.infraestructure.adapter.YamlTenantRepositoryAdapter;
import com.kuneiform.infraestructure.config.properties.WedgeConfigProperties;
import com.kuneiform.infraestructure.persistence.repository.OAuthClientJdbcRepository;
import com.kuneiform.infraestructure.persistence.repository.TenantJdbcRepository;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ConfigurationCondition.ConfigurationPhase;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Configuration for OAuth client storage. Conditionally creates beans based on the
 * wedge.client-storage.type property: - "none" (default): Uses YAML-based storage from
 * application.yaml - "postgresql", "mysql", "sqlserver": Uses database storage with specified
 * database
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ClientRepositoryConfig {

  private final WedgeConfigProperties properties;

  /**
   * Creates YAML-based client repository when client-storage.type is "none" (default). Loads
   * clients from application.yaml configuration.
   */
  @Bean
  @ConditionalOnProperty(
      name = "wedge.client-storage.type",
      havingValue = "none",
      matchIfMissing = true)
  public ClientRepository yamlClientRepository(
      PasswordEncoder passwordEncoder, TenantClientPort tenantClientPort) {
    log.info("Configuring YAML-based client storage from application.yaml");
    return new YamlClientRepositoryAdapter(properties, passwordEncoder, tenantClientPort);
  }

  /**
   * Creates YAML-based tenant repository when client-storage.type is "none" (default). Loads
   * tenants from application.yaml configuration.
   */
  @Bean
  @ConditionalOnProperty(
      name = "wedge.client-storage.type",
      havingValue = "none",
      matchIfMissing = true)
  public TenantRepository yamlTenantRepository() {
    log.info("Configuring YAML-based tenant storage from application.yaml");
    return new YamlTenantRepositoryAdapter(properties);
  }

  /** Creates DataSource for PostgreSQL client storage when client-storage.type is "postgresql". */
  @Bean(name = "clientDataSource")
  @ConditionalOnProperty(name = "wedge.client-storage.type", havingValue = "postgresql")
  public DataSource clientDataSourcePostgres() {
    log.info("Configuring PostgreSQL client storage");
    return createDataSource(properties.getClientStorage());
  }

  /** Creates DataSource for MySQL client storage when client-storage.type is "mysql". */
  @Bean(name = "clientDataSource")
  @ConditionalOnProperty(name = "wedge.client-storage.type", havingValue = "mysql")
  public DataSource clientDataSourceMysql() {
    log.info("Configuring MySQL client storage");
    return createDataSource(properties.getClientStorage());
  }

  /** Creates DataSource for SQL Server client storage when client-storage.type is "sqlserver". */
  @Bean(name = "clientDataSource")
  @ConditionalOnProperty(name = "wedge.client-storage.type", havingValue = "sqlserver")
  public DataSource clientDataSourceSqlServer() {
    log.info("Configuring SQL Server client storage");
    return createDataSource(properties.getClientStorage());
  }

  /** Custom condition that matches when any database storage type is configured. */
  static class OnDatabaseStorageCondition extends AnyNestedCondition {
    OnDatabaseStorageCondition() {
      super(ConfigurationPhase.REGISTER_BEAN);
    }

    @ConditionalOnProperty(name = "wedge.client-storage.type", havingValue = "postgresql")
    static class OnPostgreSQL {}

    @ConditionalOnProperty(name = "wedge.client-storage.type", havingValue = "mysql")
    static class OnMySQL {}

    @ConditionalOnProperty(name = "wedge.client-storage.type", havingValue = "sqlserver")
    static class OnSQLServer {}
  }

  /**
   * Configuration for Spring Data JDBC repositories. Only enabled when database storage is used.
   */
  @Configuration
  @Conditional(OnDatabaseStorageCondition.class)
  @EnableJdbcRepositories(basePackages = "com.kuneiform.infraestructure.persistence.repository")
  public static class JdbcRepositoryConfig {}

  /**
   * Creates a JdbcTemplate for the client database. This is used by JdbcDeviceStorageAdapter and
   * JdbcTenantClientAdapter.
   */
  @Bean
  @ConditionalOnBean(name = "clientDataSource")
  public org.springframework.jdbc.core.JdbcTemplate clientJdbcTemplate(
      @Autowired @org.springframework.beans.factory.annotation.Qualifier("clientDataSource")
          DataSource clientDataSource) {
    log.info("Creating JdbcTemplate for client database");
    return new org.springframework.jdbc.core.JdbcTemplate(clientDataSource);
  }

  /**
   * Creates database-backed client repository when a clientDataSource bean exists. Uses Spring Data
   * JDBC for persistence. Flyway parameter ensures migrations run before repository initialization.
   */
  @Bean
  @ConditionalOnBean(name = "clientDataSource")
  public ClientRepository databaseClientRepository(
      OAuthClientJdbcRepository repository,
      PasswordEncoder passwordEncoder,
      TenantClientPort tenantClientPort,
      WedgeConfigProperties config,
      @Autowired(required = false) Flyway flyway) {
    log.info(
        "Configuring database-backed client repository with storage type: {}",
        properties.getClientStorage().getType());
    return new DatabaseClientRepositoryAdapter(
        repository, passwordEncoder, tenantClientPort, config);
  }

  /**
   * Creates database-backed tenant repository when a clientDataSource bean exists. Uses Spring Data
   * JDBC for persistence. Flyway parameter ensures migrations run before repository initialization.
   */
  @Bean
  @ConditionalOnBean(name = "clientDataSource")
  public TenantRepository databaseTenantRepository(
      TenantJdbcRepository repository, @Autowired(required = false) Flyway flyway) {
    log.info(
        "Configuring database-backed tenant repository with storage type: {}",
        properties.getClientStorage().getType());
    return new DatabaseTenantRepositoryAdapter(repository);
  }

  /**
   * Creates a HikariCP DataSource from client storage configuration. Configures connection pool
   * settings and schema.
   */
  private DataSource createDataSource(WedgeConfigProperties.ClientStorageConfig config) {
    HikariConfig hikariConfig = new HikariConfig();
    hikariConfig.setJdbcUrl(config.getUrl());
    hikariConfig.setUsername(config.getUsername());
    hikariConfig.setPassword(config.getPassword());
    hikariConfig.setDriverClassName(config.getDriverClassName());

    // Connection pool settings
    hikariConfig.setMaximumPoolSize(config.getMaxPoolSize());
    hikariConfig.setMinimumIdle(config.getMinIdle());
    hikariConfig.setConnectionTimeout(config.getConnectionTimeout());
    hikariConfig.setIdleTimeout(config.getIdleTimeout());
    hikariConfig.setMaxLifetime(config.getMaxLifetime());
    hikariConfig.setAutoCommit(config.isAutoCommit());

    // Schema configuration
    if (config.getSchemaName() != null && !config.getSchemaName().isEmpty()) {
      hikariConfig.setSchema(config.getSchemaName());
    }

    // Pool name for monitoring
    hikariConfig.setPoolName("OAuthClientPool");

    log.info(
        "Created HikariCP DataSource: url={}, schema={}, maxPool={}, minIdle={}",
        config.getUrl(),
        config.getSchemaName(),
        config.getMaxPoolSize(),
        config.getMinIdle());

    return new HikariDataSource(hikariConfig);
  }
}
