package com.kuneiform.infraestructure.config;

import com.kuneiform.domain.model.OAuthClient;
import com.kuneiform.domain.port.ClientRepository;
import com.kuneiform.infraestructure.config.properties.WedgeConfigProperties;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class OAuth2ClientConfig {

  private final ClientRepository clientRepository;
  private final WedgeConfigProperties properties;

  /** RegisteredClientRepository that bridges our domain model to Spring's RegisteredClient. */
  @Bean
  public RegisteredClientRepository registeredClientRepository() {
    return new RegisteredClientRepository() {
      @Override
      public void save(RegisteredClient registeredClient) {
        throw new UnsupportedOperationException("Dynamic client registration not supported");
      }

      @Override
      public RegisteredClient findById(String id) {
        return findByClientId(id);
      }

      @Override
      public RegisteredClient findByClientId(String clientId) {
        Optional<OAuthClient> clientOpt = clientRepository.findByClientId(clientId);

        if (clientOpt.isEmpty()) {
          return null;
        }

        return mapToRegisteredClient(clientOpt.get());
      }
    };
  }

  private RegisteredClient mapToRegisteredClient(OAuthClient client) {
    var builder =
        RegisteredClient.withId(client.getClientId())
            .clientId(client.getClientId())
            .clientName(client.getClientName());

    if (!client.isPublic()) {
      builder.clientSecret(client.getClientSecret());
    }

    client
        .getClientAuthenticationMethods()
        .forEach(
            method -> {
              if ("none".equals(method)) {
                builder.clientAuthenticationMethod(ClientAuthenticationMethod.NONE);
              } else if ("client_secret_basic".equals(method)) {
                builder.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);
              } else if ("client_secret_post".equals(method)) {
                builder.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST);
              }
            });

    client
        .getAuthorizationGrantTypes()
        .forEach(
            grantType -> {
              if ("authorization_code".equals(grantType)) {
                builder.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE);
              } else if ("refresh_token".equals(grantType)) {
                builder.authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN);
              } else if ("client_credentials".equals(grantType)) {
                builder.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS);
              }
            });

    client.getRedirectUris().forEach(builder::redirectUri);

    // Post logout redirect URIs
    if (client.getPostLogoutRedirectUris() != null) {
      client.getPostLogoutRedirectUris().forEach(builder::postLogoutRedirectUri);
    }

    client.getScopes().forEach(builder::scope);

    ClientSettings clientSettings =
        ClientSettings.builder()
            .requireAuthorizationConsent(client.isRequireAuthorizationConsent())
            .requireProofKey(client.isRequirePkce())
            .build();

    builder.clientSettings(clientSettings);

    TokenSettings tokenSettings =
        TokenSettings.builder()
            .accessTokenTimeToLive(
                Duration.ofSeconds(properties.getOauth2().getTokens().getAccessTokenTtl()))
            .refreshTokenTimeToLive(
                Duration.ofSeconds(properties.getOauth2().getTokens().getRefreshTokenTtl()))
            .reuseRefreshTokens(false) // Enable OAuth 2.1 refresh token rotation
            .build();

    builder.tokenSettings(tokenSettings);

    RegisteredClient registeredClient = builder.build();

    // Debug logging
    log.debug(
        "Registered client: id={}, grantTypes={}, scopes={}, tokenSettings.reuseRefreshTokens={}",
        registeredClient.getClientId(),
        registeredClient.getAuthorizationGrantTypes(),
        registeredClient.getScopes(),
        registeredClient.getTokenSettings().isReuseRefreshTokens());

    return registeredClient;
  }
}
