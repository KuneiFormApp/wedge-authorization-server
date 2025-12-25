package com.kuneiform.infraestructure.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.kuneiform.domain.model.User;
import com.kuneiform.infraestructure.config.RedisConfig;
import com.kuneiform.infraestructure.config.properties.WedgeConfigProperties;
import com.redis.testcontainers.RedisContainer;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(
    classes = {RedisOAuth2AuthorizationServiceAdapter.class, RedisConfig.class},
    properties = {"wedge.token-storage.type=redis", "wedge.session.storage-type=redis"})
@EnableConfigurationProperties(WedgeConfigProperties.class)
@Testcontainers
class RedisOAuth2AuthorizationServiceAdapterIT {

  @Container
  static final RedisContainer REDIS =
      new RedisContainer(DockerImageName.parse("redis:7.2.4-alpine"));

  @Autowired private RedisOAuth2AuthorizationServiceAdapter adapter;

  @Autowired
  private org.springframework.data.redis.connection.RedisConnectionFactory connectionFactory;

  private RedisTemplate<String, OAuth2Authorization> redisAuthTemplate;

  @DynamicPropertySource
  static void redisProperties(DynamicPropertyRegistry registry) {
    registry.add("wedge.session.redis.host", REDIS::getHost);
    registry.add("wedge.session.redis.port", REDIS::getFirstMappedPort);
    // Overwrite any defaults to empty for Testcontainers (which has no password by default)
    registry.add("wedge.session.redis.username", () -> "");
    registry.add("wedge.session.redis.password", () -> "");
    // Set a known namespace for testing
    registry.add("wedge.token-storage.redis.namespace", () -> "wedge:test:oauth2:");
  }

  private RegisteredClient registeredClient;
  private User userPrincipal;

  @BeforeEach
  void setUp() {
    // Manually create template for verification
    redisAuthTemplate = new RedisTemplate<>();
    redisAuthTemplate.setConnectionFactory(connectionFactory);
    redisAuthTemplate.setKeySerializer(
        new org.springframework.data.redis.serializer.StringRedisSerializer());
    redisAuthTemplate.setValueSerializer(
        new org.springframework.data.redis.serializer.JdkSerializationRedisSerializer());
    redisAuthTemplate.afterPropertiesSet();

    registeredClient =
        RegisteredClient.withId("client-1")
            .clientId("client-1")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("http://localhost:8080/callback")
            .build();

    userPrincipal =
        User.builder()
            .userId("user-1")
            .username("testuser")
            .email("test@example.com")
            .metadata(Map.of("role", "user"))
            .build();

    if (redisAuthTemplate.getConnectionFactory() != null) {
      redisAuthTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }
  }

  @Test
  void saveAndFindById() {
    Authentication principal = new UsernamePasswordAuthenticationToken(userPrincipal, "password");

    OAuth2Authorization authorization =
        OAuth2Authorization.withRegisteredClient(registeredClient)
            .principalName(userPrincipal.getUsername())
            .attribute(java.security.Principal.class.getName(), principal)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .id("auth-1")
            .build();

    adapter.save(authorization);

    OAuth2Authorization result = adapter.findById("auth-1");
    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo("auth-1");

    // Verify stored in Redis (namespace + AUTH_KEY_PREFIX + id)
    assertThat(redisAuthTemplate.hasKey("wedge:test:oauth2:auth:auth-1")).isTrue();
  }

  @Test
  void saveAndFindByToken() {
    OAuth2AccessToken accessToken =
        new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            "access-token-123",
            Instant.now(),
            Instant.now().plus(1, ChronoUnit.HOURS));

    Authentication principal = new UsernamePasswordAuthenticationToken(userPrincipal, "password");

    OAuth2Authorization authorization =
        OAuth2Authorization.withRegisteredClient(registeredClient)
            .principalName(userPrincipal.getUsername())
            .attribute(java.security.Principal.class.getName(), principal)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .id("auth-1")
            .token(accessToken)
            .build();

    adapter.save(authorization);

    OAuth2Authorization result =
        adapter.findByToken("access-token-123", OAuth2TokenType.ACCESS_TOKEN);

    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo("auth-1");
  }

  @Test
  void remove() {
    Authentication principal = new UsernamePasswordAuthenticationToken(userPrincipal, "password");

    OAuth2Authorization authorization =
        OAuth2Authorization.withRegisteredClient(registeredClient)
            .principalName(userPrincipal.getUsername())
            .attribute(java.security.Principal.class.getName(), principal)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .id("auth-1")
            .build();

    adapter.save(authorization);
    adapter.remove(authorization);

    OAuth2Authorization result = adapter.findById("auth-1");
    assertThat(result).isNull();
    assertThat(redisAuthTemplate.hasKey("wedge:test:oauth2:auth:auth-1")).isFalse();
  }
}
