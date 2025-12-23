package com.kuneiform.infraestructure.config.properties;

import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "wedge")
public class WedgeConfigProperties {

  private UserProviderConfig userProvider = new UserProviderConfig();
  private List<ClientConfig> clients = List.of();
  private SessionConfig session = new SessionConfig();
  private JwtConfig jwt = new JwtConfig();
  private List<ScopeConfig> scopes = List.of();

  @Data
  public static class UserProviderConfig {
    private boolean enabled = true;
    private String endpoint;
    private int timeout = 5000;
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
  }

  @Data
  public static class SessionConfig {
    private String storageType = "in-memory";
    private int ttl = 600; // seconds
    private int maxSize = 10000; // max sessions in cache (for in-memory storage)
    private RedisConfig redis = new RedisConfig();
  }

  @Data
  public static class RedisConfig {
    private String host = "localhost";
    private int port = 6379;
    private String username = "";
    private String password = "";
    private int database = 0;
    private String namespace = "wedge:session";
    private SslConfig ssl = new SslConfig();

    @Data
    public static class SslConfig {
      private boolean enabled = false;
    }
  }

  @Data
  public static class JwtConfig {
    private String issuer;
    private String keyType = "rsa";
    private int keySize = 2048;
  }

  @Data
  public static class ScopeConfig {
    private String name;
    private String description;
  }
}
