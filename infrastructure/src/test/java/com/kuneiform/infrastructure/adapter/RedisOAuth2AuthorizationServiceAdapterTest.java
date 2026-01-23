package com.kuneiform.infrastructure.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kuneiform.infrastructure.config.properties.WedgeConfigProperties;
import com.kuneiform.infrastructure.config.properties.WedgeConfigProperties.RedisConfig;
import com.kuneiform.infrastructure.config.properties.WedgeConfigProperties.TokenStorageConfig;
import java.time.Duration;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;

@ExtendWith(MockitoExtension.class)
class RedisOAuth2AuthorizationServiceAdapterTest {

  @Mock private WedgeConfigProperties config;
  @Mock private RedisConnectionFactory redisConnectionFactory;
  @Mock private RedisTemplate<String, OAuth2Authorization> redisAuthTemplate;
  @Mock private RedisTemplate<String, String> redisIndexTemplate;
  @Mock private ValueOperations<String, OAuth2Authorization> authValueOps;
  @Mock private ValueOperations<String, String> indexValueOps;

  private RedisOAuth2AuthorizationServiceAdapter adapter;

  @BeforeEach
  void setUp() {
    // Setup generic config mocks
    TokenStorageConfig tokenStorageConfig = mock(TokenStorageConfig.class);
    RedisConfig redisConfig = mock(RedisConfig.class);
    when(config.getTokenStorage()).thenReturn(tokenStorageConfig);
    when(tokenStorageConfig.getMaxTtl()).thenReturn(3600L);
    when(tokenStorageConfig.getMaxSize()).thenReturn(100);
    when(tokenStorageConfig.getRedis()).thenReturn(redisConfig);
    when(redisConfig.getNamespace()).thenReturn("wedge:test");

    // Mock Redis ops
    org.mockito.Mockito.lenient().doReturn(authValueOps).when(redisAuthTemplate).opsForValue();
    org.mockito.Mockito.lenient().doReturn(indexValueOps).when(redisIndexTemplate).opsForValue();

    // Use constructor injection directly, which matches the @RequiredArgsConstructor with final
    // fields
    // Constructor signature: (WedgeConfigProperties, RedisTemplate, RedisTemplate)
    adapter =
        new RedisOAuth2AuthorizationServiceAdapter(config, redisAuthTemplate, redisIndexTemplate);

    adapter.init();
  }

  @Test
  void shouldSaveAuthorizationWithL1AndL2Caching() {
    // Create real objects instead of mocks for final classes
    org.springframework.security.oauth2.server.authorization.client.RegisteredClient
        registeredClient =
            org.springframework.security.oauth2.server.authorization.client.RegisteredClient.withId(
                    "client-1")
                .clientId("client-1")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost")
                .build();

    OAuth2AccessToken accessToken =
        new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            "access-token-val",
            java.time.Instant.now(),
            java.time.Instant.now().plusSeconds(300));

    OAuth2Authorization authorization =
        OAuth2Authorization.withRegisteredClient(registeredClient)
            .principalName("user-1")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .accessToken(accessToken)
            .authorizedScopes(Collections.emptySet())
            .id("auth-1")
            .build();

    adapter.save(authorization);

    // Verify L2 write
    verify(authValueOps).set(eq("wedge:test:auth:auth-1"), eq(authorization), any(Duration.class));

    // Verify Index write (access token)
    verify(indexValueOps)
        .set(
            eq("wedge:test:index:access_token:access-token-val"),
            eq("auth-1"),
            any(Duration.class));
  }

  @Test
  void shouldFindByIdFromL2IfL1Miss() {
    String id = "auth-miss";
    OAuth2Authorization authorization = mock(OAuth2Authorization.class);

    when(authValueOps.get("wedge:test:auth:" + id)).thenReturn(authorization);

    OAuth2Authorization result = adapter.findById(id);

    assertThat(result).isEqualTo(authorization);
  }

  @Test
  void shouldRemoveAuthorization() {
    String id = "auth-remove";
    OAuth2Authorization authorization = mock(OAuth2Authorization.class);
    when(authorization.getId()).thenReturn(id);

    adapter.remove(authorization);

    verify(redisAuthTemplate).delete("wedge:test:auth:" + id);
  }

  @Test
  void shouldFindByTokenUsingIndex() {
    String tokenValue = "some-token";
    OAuth2TokenType tokenType = OAuth2TokenType.ACCESS_TOKEN;
    String authId = "found-auth-id";
    OAuth2Authorization authorization = mock(OAuth2Authorization.class);

    // Index lookup stub
    when(indexValueOps.get("wedge:test:index:access_token:" + tokenValue)).thenReturn(authId);

    // Auth lookup stub
    when(authValueOps.get("wedge:test:auth:" + authId)).thenReturn(authorization);

    OAuth2Authorization result = adapter.findByToken(tokenValue, tokenType);

    assertThat(result).isEqualTo(authorization);
  }
}
