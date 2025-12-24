package com.kuneiform.infraestructure.config;

import com.kuneiform.infraestructure.config.properties.WedgeConfigProperties;
import com.kuneiform.infraestructure.security.HttpUserAuthenticationProvider;
import com.kuneiform.infraestructure.security.PublicClientRefreshTokenAuthenticationProvider;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.StringUtils;

@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final WedgeConfigProperties config;
  private final PublicClientRefreshTokenAuthenticationProvider
      publicClientRefreshTokenAuthenticationProvider;

  @Bean
  @Order(1)
  public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http)
      throws Exception {
    OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
        new OAuth2AuthorizationServerConfigurer();

    // Matcher for all OAuth2 endpoints (authorize, token, jwk, etc.)
    RequestMatcher endpointsMatcher = authorizationServerConfigurer.getEndpointsMatcher();

    http.securityMatcher(endpointsMatcher)
        .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
        .csrf(csrf -> csrf.ignoringRequestMatchers(endpointsMatcher))
        .with(
            authorizationServerConfigurer,
            configurer ->
                configurer
                    .clientAuthentication(
                        clientAuth -> {
                          clientAuth.authenticationConverter(
                              new org.springframework.security.web.authentication
                                  .DelegatingAuthenticationConverter(
                                  java.util.Arrays.asList(
                                      new org.springframework.security.oauth2.server.authorization
                                          .web.authentication
                                          .JwtClientAssertionAuthenticationConverter(),
                                      new org.springframework.security.oauth2.server.authorization
                                          .web.authentication
                                          .ClientSecretBasicAuthenticationConverter(),
                                      new org.springframework.security.oauth2.server.authorization
                                          .web.authentication
                                          .ClientSecretPostAuthenticationConverter(),
                                      new org.springframework.security.oauth2.server.authorization
                                          .web.authentication.PublicClientAuthenticationConverter(),
                                      // Custom converter for Public Client Refresh Token Grant
                                      request -> {
                                        String grantType =
                                            request.getParameter(OAuth2ParameterNames.GRANT_TYPE);
                                        String clientId =
                                            request.getParameter(OAuth2ParameterNames.CLIENT_ID);
                                        String clientSecret =
                                            request.getParameter(
                                                OAuth2ParameterNames.CLIENT_SECRET);

                                        if (AuthorizationGrantType.REFRESH_TOKEN
                                                .getValue()
                                                .equals(grantType)
                                            && StringUtils.hasText(clientId)
                                            && !StringUtils.hasText(clientSecret)) {

                                          // Pass grant_type in parameters so the provider knows
                                          // context
                                          Map<String, Object> params = new HashMap<>();
                                          params.put(OAuth2ParameterNames.GRANT_TYPE, grantType);

                                          return new OAuth2ClientAuthenticationToken(
                                              clientId,
                                              ClientAuthenticationMethod.NONE,
                                              null,
                                              params);
                                        }
                                        return null;
                                      })));

                          // Add custom provider for Public Client Refresh Token flow
                          clientAuth.authenticationProvider(
                              publicClientRefreshTokenAuthenticationProvider);
                        })
                    .authorizationEndpoint(Customizer.withDefaults())
                    .tokenEndpoint(Customizer.withDefaults())
                    .oidc(Customizer.withDefaults()))
        .exceptionHandling(
            exceptions ->
                exceptions.defaultAuthenticationEntryPointFor(
                    new LoginUrlAuthenticationEntryPoint("/login"),
                    new MediaTypeRequestMatcher(MediaType.TEXT_HTML)));

    return http.build();
  }

  @Bean
  @Order(2)
  public SecurityFilterChain defaultSecurityFilterChain(
      HttpSecurity http, HttpUserAuthenticationProvider authenticationProvider) throws Exception {
    http.authorizeHttpRequests(
            authorize ->
                authorize
                    .requestMatchers("/login", "/error")
                    .permitAll() // Explicitly permit login and error pages
                    .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico")
                    .permitAll() // Allow static resources
                    .requestMatchers("/actuator/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .formLogin(form -> form.loginPage("/login").permitAll().failureUrl("/login?error=true"))
        .logout(logout -> logout.logoutSuccessUrl("/login?logout=true").permitAll())
        .authenticationProvider(authenticationProvider);

    return http.build();
  }

  // JWK source for JWT signing
  @Bean
  public JWKSource<SecurityContext> jwkSource() {
    KeyPair keyPair = generateRsaKey();
    RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
    RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

    RSAKey rsaKey =
        new RSAKey.Builder(publicKey)
            .privateKey(privateKey)
            .keyID(UUID.randomUUID().toString())
            .build();

    JWKSet jwkSet = new JWKSet(rsaKey);
    return new ImmutableJWKSet<>(jwkSet);
  }

  private KeyPair generateRsaKey() {
    try {
      KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
      keyPairGenerator.initialize(config.getJwt().getKeySize());
      KeyPair keyPair = keyPairGenerator.generateKeyPair();
      log.info(
          "Generated RSA key pair for JWT signing (key size: {})", config.getJwt().getKeySize());
      return keyPair;
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to generate RSA key pair", ex);
    }
  }

  // JWT decoder for validating tokens
  @Bean
  public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
    return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
  }

  @Bean
  public AuthorizationServerSettings authorizationServerSettings() {
    return AuthorizationServerSettings.builder().issuer(config.getJwt().getIssuer()).build();
  }
}
