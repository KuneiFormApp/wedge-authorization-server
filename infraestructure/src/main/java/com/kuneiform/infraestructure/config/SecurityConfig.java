package com.kuneiform.infraestructure.config;

import com.kuneiform.infraestructure.config.properties.WedgeConfigProperties;
import com.kuneiform.infraestructure.security.HttpUserAuthenticationProvider;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
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
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final WedgeConfigProperties config;

  @Bean
  @Order(1)
  public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http)
      throws Exception {
    // Apply and configure OAuth2 Authorization Server
    OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
        new OAuth2AuthorizationServerConfigurer();

    http.with(
        authorizationServerConfigurer,
        configurer -> configurer.oidc(Customizer.withDefaults())); // Enable OpenID
    // Connect 1.0

    http.securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
        .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
        .exceptionHandling(
            exceptions ->
                exceptions.defaultAuthenticationEntryPointFor(
                    new LoginUrlAuthenticationEntryPoint("/login"),
                    new MediaTypeRequestMatcher(MediaType.TEXT_HTML)))
        .oauth2ResourceServer(resourceServer -> resourceServer.jwt(Customizer.withDefaults()));

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
                    .requestMatchers("/css/**", "/js/**", "/images/**")
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
