package com.kuneiform.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

class PublicClientRefreshTokenAuthenticationProviderTest {

  private PublicClientRefreshTokenAuthenticationProvider provider;
  private RegisteredClientRepository registeredClientRepository;
  private OAuth2AuthorizationService authorizationService;

  @BeforeEach
  void setUp() {
    registeredClientRepository = mock(RegisteredClientRepository.class);
    authorizationService = mock(OAuth2AuthorizationService.class);
    provider = new PublicClientRefreshTokenAuthenticationProvider(registeredClientRepository);
  }

  @Test
  void authenticate_Success() {
    RegisteredClient registeredClient =
        RegisteredClient.withId("client-1")
            .clientId("public-client")
            .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
            .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
            .redirectUri("http://localhost:8080/callback")
            .build();

    when(registeredClientRepository.findByClientId("public-client")).thenReturn(registeredClient);

    OAuth2ClientAuthenticationToken authentication =
        new OAuth2ClientAuthenticationToken(
            "public-client",
            ClientAuthenticationMethod.NONE,
            null,
            Map.of(
                OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.REFRESH_TOKEN.getValue()));

    Authentication result = provider.authenticate(authentication);

    assertThat(result).isNotNull();
    assertThat(result.isAuthenticated()).isTrue();
    assertThat(result.getPrincipal()).isEqualTo("public-client");
  }

  @Test
  void authenticate_InvalidClientId() {
    when(registeredClientRepository.findByClientId("invalid-client")).thenReturn(null);

    OAuth2ClientAuthenticationToken authentication =
        new OAuth2ClientAuthenticationToken(
            "invalid-client",
            ClientAuthenticationMethod.NONE,
            null,
            Map.of(
                OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.REFRESH_TOKEN.getValue()));

    assertThatThrownBy(() -> provider.authenticate(authentication))
        .isInstanceOf(OAuth2AuthenticationException.class)
        .extracting(e -> ((OAuth2AuthenticationException) e).getError().getErrorCode())
        .isEqualTo(OAuth2ErrorCodes.INVALID_CLIENT);
  }

  @Test
  void authenticate_NotPublicClient() {
    RegisteredClient confidentialClient =
        RegisteredClient.withId("client-2")
            .clientId("confidential-client")
            .clientSecret("secret")
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
            .redirectUri("http://localhost:8080/callback")
            .build();

    when(registeredClientRepository.findByClientId("confidential-client"))
        .thenReturn(confidentialClient);

    OAuth2ClientAuthenticationToken authentication =
        new OAuth2ClientAuthenticationToken(
            "confidential-client",
            ClientAuthenticationMethod.NONE,
            null,
            Map.of(
                OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.REFRESH_TOKEN.getValue()));

    assertThatThrownBy(() -> provider.authenticate(authentication))
        .isInstanceOf(OAuth2AuthenticationException.class)
        .extracting(e -> ((OAuth2AuthenticationException) e).getError().getErrorCode())
        .isEqualTo(OAuth2ErrorCodes.INVALID_CLIENT);
  }
}
