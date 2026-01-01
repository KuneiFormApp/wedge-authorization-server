package com.kuneiform.infraestructure.config.properties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "wedge")
public class WedgeConfigProperties {

  private List<TenantConfig> tenants = new ArrayList<>();
  private List<ClientConfig> clients = new ArrayList<>();
  private ClientStorageConfig clientStorage = new ClientStorageConfig();
  private SessionConfig session = new SessionConfig();
  private OAuth2Config oauth2 = new OAuth2Config();
  private TokenStorageConfig tokenStorage = new TokenStorageConfig();
  private JwtConfig jwt = new JwtConfig();
  private List<String> scopes =
      new ArrayList<>(
          Arrays.asList("openid", "profile", "email", "read", "write", "admin", "offline_access"));
  private FrontendConfig frontend = new FrontendConfig();

  @Data
  public static class TenantConfig {
    private String id;
    private String name;
    private UserProviderConfig userProvider;
  }

  @Data
  public static class UserProviderConfig {
    private String endpoint;
    private int timeout = 5000;
  }

  @Data
  public static class ClientStorageConfig {
    private String type = "none"; // Options: none, postgresql, mysql, sqlserver
    private String url;
    private String username;
    private String password;
    private String driverClassName;
    private String schemaName = "public"; // Default schema name
    private boolean autoCreateSchema = false; // Auto-create schema if it doesn't exist

    // HikariCP connection pool settings
    private int maxPoolSize = 10;
    private int minIdle = 5;
    private long connectionTimeout = 30000; // 30 seconds in milliseconds
    private long idleTimeout = 600000; // 10 minutes in milliseconds
    private long maxLifetime = 1800000; // 30 minutes in milliseconds
    private boolean autoCommit = true;
  }

  @Data
  public static class ClientConfig {
    private String clientId;
    private String clientSecret;
    private String clientName;
    private List<String> clientAuthenticationMethods;
    private List<String> authorizationGrantTypes;
    private List<String> redirectUris;
    private List<String> postLogoutRedirectUris;
    private List<String> scopes;
    private boolean requireAuthorizationConsent;
    private boolean requirePkce;
    private String tenantId;
  }

  @Data
  public static class SessionConfig {
    private String storageType = "in-memory";
    private int authTtl = 600; // time-to-live for PKCE Auth sessions (short-lived)
    private int httpTtl = 1800; // time-to-live for HTTP User sessions (long-lived)
    private int maxSize = 10000; // max sessions in cache (for in-memory storage)
    private RedisConfig redis = new RedisConfig();
  }

  @Data
  public static class RedisConfig {
    private String host = "127.0.0.1";
    private int port = 6379;
    private String username = "";
    private String password = "";
    private int database = 0;
    private String namespace =
        "wedge:session"; // Used for wedge:auth:session by default logic if not overridden
    private String httpNamespace = "wedge:http:session"; // Used for Spring Session
    private SslConfig ssl = new SslConfig();

    @Data
    public static class SslConfig {
      private boolean enabled = false;
    }
  }

  @Data
  public static class JwtConfig {
    private String issuer;
    private String keyType = "test";
    private int keySize = 2048;
    private String privateKeyPath;
    private String publicKeyPath;
  }

  @Data
  public static class OAuth2Config {
    private TokenConfig tokens = new TokenConfig();

    @Data
    public static class TokenConfig {
      private boolean refreshTokenEnabled = true; // Enable/disable refresh tokens
      private long accessTokenTtl = 1800; // 30 minutes
      private long refreshTokenTtl = 2592000; // 30 days
    }
  }

  @Data
  public static class TokenStorageConfig {
    private String type = "in-memory";
    private long maxTtl = 2592000; // Max 30 days
    private int maxSize = 50000;
    private RedisConfig redis = new RedisConfig();
  }

  @Data
  public static class FrontendConfig {
    /** Optional external templates directory path (e.g., file:///path/to/templates) */
    private String templatesPath;

    /** Optional external static resources directory path (e.g., file:///path/to/static) */
    private String staticPath;

    /** i18n messages location (default: classpath:i18n/messages) */
    private String i18nBasename = "classpath:i18n/messages";

    /** Default locale (default: en) */
    private String defaultLocale = "en";

    /** Supported locales (default: en, es) */
    private List<String> supportedLocales = new ArrayList<>(Arrays.asList("en", "es"));
  }
}
