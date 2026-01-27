package com.kuneiform.infrastructure.config;

import com.kuneiform.infrastructure.config.properties.WedgeConfigProperties;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.time.Instant;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.DelegatingOAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.JwtGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2AccessTokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.util.CollectionUtils;

/**
 * OAuth2 Token Generator Configuration that enables refresh tokens for public clients.
 *
 * <p>Overrides Spring Authorization Server's default behavior which blocks refresh tokens for
 * public clients (ClientAuthenticationMethod.NONE).
 */
@Slf4j
@Configuration
public class OAuth2TokenGeneratorConfig {

  @Bean
  @Primary
  public OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator(
      JWKSource<SecurityContext> jwkSource,
      OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer,
      WedgeConfigProperties config) {

    log.info("★★★ Creating CUSTOM OAuth2TokenGenerator with public client support ★★★");
    log.info(
        "    Refresh tokens enabled: {}", config.getOauth2().getTokens().isRefreshTokenEnabled());

    JwtGenerator jwtGenerator = new JwtGenerator(new NimbusJwtEncoder(jwkSource));
    jwtGenerator.setJwtCustomizer(tokenCustomizer);

    OAuth2AccessTokenGenerator accessTokenGenerator = new OAuth2AccessTokenGenerator();

    // Custom refresh token generator that creates tokens directly for public
    // clients
    PublicClientRefreshTokenGenerator refreshTokenGenerator =
        new PublicClientRefreshTokenGenerator(config);

    log.info("Token generators configured:");
    log.info("  1. JwtGenerator (for access tokens)");
    log.info("  2. OAuth2AccessTokenGenerator");
    log.info("  3. PublicClientRefreshTokenGenerator (CUSTOM)");

    DelegatingOAuth2TokenGenerator tokenGenerator =
        new DelegatingOAuth2TokenGenerator(
            jwtGenerator, accessTokenGenerator, refreshTokenGenerator);

    log.info("DelegatingOAuth2TokenGenerator created with {} generators", 3);

    return tokenGenerator;
  }

  /**
   * Custom Refresh Token Generator that allows public clients with PKCE to receive refresh tokens.
   *
   * <p>Wraps the default generator and adds additional checks for offline_access scope.
   */
  private static class PublicClientRefreshTokenGenerator
      implements OAuth2TokenGenerator<OAuth2RefreshToken> {

    private final WedgeConfigProperties config;

    public PublicClientRefreshTokenGenerator(WedgeConfigProperties config) {
      this.config = config;
      log.info(
          "PublicClientRefreshTokenGenerator initialized (direct token creation, enabled: {})",
          config.getOauth2().getTokens().isRefreshTokenEnabled());
    }

    @Override
    public OAuth2RefreshToken generate(OAuth2TokenContext context) {
      // Check if refresh tokens are globally enabled
      if (!config.getOauth2().getTokens().isRefreshTokenEnabled()) {
        log.debug("Refresh tokens are disabled in configuration");
        return null;
      }

      // Only generate for refresh token type
      if (!OAuth2TokenType.REFRESH_TOKEN.equals(context.getTokenType())) {
        return null;
      }

      // Only for authorization_code or refresh_token grant types
      AuthorizationGrantType grantType = context.getAuthorizationGrantType();
      if (!AuthorizationGrantType.AUTHORIZATION_CODE.equals(grantType)
          && !AuthorizationGrantType.REFRESH_TOKEN.equals(grantType)) {
        return null;
      }

      // Check if offline_access scope was requested
      Set<String> authorizedScopes = context.getAuthorizedScopes();
      if (CollectionUtils.isEmpty(authorizedScopes)
          || !authorizedScopes.contains("offline_access")) {
        log.debug(
            "Refresh token not generated: offline_access scope not requested for client {}",
            context.getRegisteredClient().getClientId());
        return null;
      }

      // Check if client has REFRESH_TOKEN grant type configured
      Set<AuthorizationGrantType> clientGrantTypes =
          context.getRegisteredClient().getAuthorizationGrantTypes();
      if (!clientGrantTypes.contains(AuthorizationGrantType.REFRESH_TOKEN)) {
        log.debug(
            "Refresh token not generated: REFRESH_TOKEN grant type not configured for client {}",
            context.getRegisteredClient().getClientId());
        return null;
      }

      // Create refresh token directly (bypassing Spring's public client restriction)
      Instant issuedAt = Instant.now();
      Instant expiresAt =
          issuedAt.plus(
              context.getRegisteredClient().getTokenSettings().getRefreshTokenTimeToLive());

      OAuth2RefreshToken refreshToken =
          new OAuth2RefreshToken(generateTokenValue(), issuedAt, expiresAt);

      log.debug(
          "Generated refresh token for client: {}, rotation enabled: {}, ttl: {}s",
          context.getRegisteredClient().getClientId(),
          !context.getRegisteredClient().getTokenSettings().isReuseRefreshTokens(),
          context
              .getRegisteredClient()
              .getTokenSettings()
              .getRefreshTokenTimeToLive()
              .getSeconds());

      return refreshToken;
    }

    private String generateTokenValue() {
      // Generate a secure random token value (similar to Spring's implementation)
      return Base64.getUrlEncoder()
          .withoutPadding()
          .encodeToString(UUID.randomUUID().toString().getBytes());
    }
  }
}
