package com.kuneiform.infrastructure.config;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kuneiform.domain.port.JwtKeyProvider;
import com.kuneiform.infrastructure.config.properties.WedgeConfigProperties;
import com.kuneiform.infrastructure.security.HttpUserAuthenticationProvider;
import com.kuneiform.infrastructure.security.MfaAwareAuthenticationFailureHandler;
import com.kuneiform.infrastructure.security.OAuth2AuthorizationRevocationLogoutHandler;
import com.kuneiform.infrastructure.security.PublicClientRefreshTokenAuthenticationProvider;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = {SecurityConfig.class, SecurityConfigIT.TestMocks.class})
@ActiveProfiles("test")
class SecurityConfigIT {

  @TestConfiguration
  static class TestMocks {
    @Bean
    @Primary
    WedgeConfigProperties wedgeConfigProperties() {
      WedgeConfigProperties props = mock(WedgeConfigProperties.class);
      WedgeConfigProperties.JwtConfig jwt = mock(WedgeConfigProperties.JwtConfig.class);
      WedgeConfigProperties.MultiTenancyConfig mt =
          mock(WedgeConfigProperties.MultiTenancyConfig.class);

      when(props.getJwt()).thenReturn(jwt);
      when(jwt.getIssuer()).thenReturn("http://localhost:9000");
      when(props.getMultiTenancy()).thenReturn(mt);
      when(mt.isEnabled()).thenReturn(false);
      return props;
    }

    @Bean
    @Primary
    JwtKeyProvider jwtKeyProvider() {
      try {
        java.security.KeyPairGenerator keyPairGenerator =
            java.security.KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        java.security.KeyPair generatedKeyPair = keyPairGenerator.generateKeyPair();

        com.kuneiform.domain.model.JwtKeyPair keyPair =
            new com.kuneiform.domain.model.JwtKeyPair(
                (java.security.interfaces.RSAPublicKey) generatedKeyPair.getPublic(),
                (java.security.interfaces.RSAPrivateKey) generatedKeyPair.getPrivate(),
                "test-key-id");

        JwtKeyProvider provider = mock(JwtKeyProvider.class);
        when(provider.getKeyPair()).thenReturn(keyPair);
        return provider;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    @Bean
    @Primary
    PublicClientRefreshTokenAuthenticationProvider
        publicClientRefreshTokenAuthenticationProvider() {
      return mock(PublicClientRefreshTokenAuthenticationProvider.class);
    }

    @Bean
    @Primary
    HttpUserAuthenticationProvider httpUserAuthenticationProvider() {
      return mock(HttpUserAuthenticationProvider.class);
    }

    @Bean
    @Primary
    OAuth2AuthorizationRevocationLogoutHandler logoutHandler() {
      return mock(OAuth2AuthorizationRevocationLogoutHandler.class);
    }

    @Bean
    @Primary
    MfaAwareAuthenticationFailureHandler mfaFailureHandler() {
      return mock(MfaAwareAuthenticationFailureHandler.class);
    }

    @Bean
    @Primary
    org.springframework.jdbc.core.JdbcTemplate jdbcTemplate() {
      return mock(org.springframework.jdbc.core.JdbcTemplate.class);
    }

    @Bean
    @Primary
    org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
        registeredClientRepository() {
      return mock(
          org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
              .class);
    }
  }

  @Test
  void contextLoads() {
    // Context load verification
  }
}
