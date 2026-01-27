package com.kuneiform.infrastructure.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kuneiform.infrastructure.config.properties.WedgeConfigProperties;
import com.kuneiform.infrastructure.config.properties.WedgeConfigProperties.TokenStorageConfig;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

class InMemoryOAuth2AuthorizationServiceAdapterTest {

  private InMemoryOAuth2AuthorizationServiceAdapter service;
  private WedgeConfigProperties config;
  private RegisteredClient registeredClient;

  @BeforeEach
  void setUp() {
    config = mock(WedgeConfigProperties.class);
    TokenStorageConfig tokenStorageConfig = mock(TokenStorageConfig.class);

    when(config.getTokenStorage()).thenReturn(tokenStorageConfig);
    when(tokenStorageConfig.getMaxTtl()).thenReturn(3600L); // 1 hour
    when(tokenStorageConfig.getMaxSize()).thenReturn(100);

    service = new InMemoryOAuth2AuthorizationServiceAdapter(config);
    service.init();

    registeredClient =
        RegisteredClient.withId("client-1")
            .clientId("client-1")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("http://localhost:8080/callback")
            .build();
  }

  @Test
  void saveAndFindById() {
    OAuth2Authorization authorization =
        OAuth2Authorization.withRegisteredClient(registeredClient)
            .principalName("user-1")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .id("auth-1")
            .build();

    service.save(authorization);

    OAuth2Authorization result = service.findById("auth-1");
    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo("auth-1");
  }

  @Test
  void saveAndFindByToken() {
    OAuth2AccessToken accessToken =
        new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            "access-token-123",
            Instant.now(),
            Instant.now().plus(1, ChronoUnit.HOURS));

    OAuth2Authorization authorization =
        OAuth2Authorization.withRegisteredClient(registeredClient)
            .principalName("user-1")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .id("auth-1")
            .token(accessToken)
            .build();

    service.save(authorization);

    OAuth2Authorization result =
        service.findByToken("access-token-123", OAuth2TokenType.ACCESS_TOKEN);

    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo("auth-1");
  }

  @Test
  void remove() {
    OAuth2Authorization authorization =
        OAuth2Authorization.withRegisteredClient(registeredClient)
            .principalName("user-1")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .id("auth-1")
            .build();

    service.save(authorization);
    service.remove(authorization);

    OAuth2Authorization result = service.findById("auth-1");
    assertThat(result).isNull();
  }

  @Test
  void findByUserId() {
    OAuth2Authorization authorization =
        OAuth2Authorization.withRegisteredClient(registeredClient)
            .principalName("user-123") // This is the userId
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .id("auth-1")
            .build();

    service.save(authorization);

    OAuth2Authorization result = service.findByUserId("user-123");
    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo("auth-1");
    assertThat(result.getPrincipalName()).isEqualTo("user-123");
  }

  @Test
  void findByUserId_shouldReturnNull_whenUserIdNotFound() {
    OAuth2Authorization result = service.findByUserId("non-existent-user");
    assertThat(result).isNull();
  }

  @Test
  void findByUserId_shouldReturnNull_whenUserIdIsNull() {
    OAuth2Authorization result = service.findByUserId(null);
    assertThat(result).isNull();
  }

  @Test
  void remove_shouldClearUserIdIndex() {
    OAuth2Authorization authorization =
        OAuth2Authorization.withRegisteredClient(registeredClient)
            .principalName("user-456")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .id("auth-2")
            .build();

    service.save(authorization);

    // Verify it's findable by userId
    assertThat(service.findByUserId("user-456")).isNotNull();

    // Remove it
    service.remove(authorization);

    // Verify it's no longer findable by userId
    assertThat(service.findByUserId("user-456")).isNull();
  }
}
