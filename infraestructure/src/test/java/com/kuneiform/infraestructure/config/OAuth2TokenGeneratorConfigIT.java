package com.kuneiform.infraestructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.kuneiform.infraestructure.config.properties.WedgeConfigProperties;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

/**
 * Integration test for OAuth2TokenGeneratorConfig.
 *
 * <p>This test verifies that the OAuth2TokenGenerator is properly configured with custom token
 * customizers (like WedgeTokenCustomizer).
 */
@SpringBootTest(
    classes = {OAuth2TokenGeneratorConfig.class, OAuth2TokenGeneratorConfigIT.TestConfig.class})
@EnableConfigurationProperties(WedgeConfigProperties.class)
class OAuth2TokenGeneratorConfigIT {

  @TestConfiguration
  static class TestConfig {
    @Bean
    public JWKSource<SecurityContext> jwkSource() {
      // Generate a test RSA key pair
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

    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer() {
      // Simple no-op customizer for testing
      return context -> {
        // No-op customizer for testing purposes
      };
    }

    private static KeyPair generateRsaKey() {
      try {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        return keyPairGenerator.generateKeyPair();
      } catch (Exception ex) {
        throw new IllegalStateException(ex);
      }
    }
  }

  @Autowired private ApplicationContext context;

  @Test
  void contextLoadsWithTokenGeneratorConfig() {
    // Verify the application context loads with OAuth2TokenGeneratorConfig
    // If this passes, it means all required dependencies were satisfied
    assertThat(context).isNotNull();
  }

  @Test
  void tokenGeneratorBeanIsConfigured() {
    // Verify the OAuth2TokenGenerator bean exists and can be retrieved
    OAuth2TokenGenerator<?> tokenGenerator = context.getBean(OAuth2TokenGenerator.class);
    assertThat(tokenGenerator).isNotNull();
  }
}
