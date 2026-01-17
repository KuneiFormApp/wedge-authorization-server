package com.kuneiform.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import org.junit.jupiter.api.Test;

class OAuthClientTest {

  @Test
  void shouldBuildOAuthClient() {
    OAuthClient client =
        OAuthClient.builder()
            .clientId("test-client")
            .clientSecret("secret")
            .clientName("Test Client")
            .clientAuthenticationMethods(Set.of("client_secret_basic"))
            .authorizationGrantTypes(Set.of("authorization_code", "refresh_token"))
            .redirectUris(Set.of("http://localhost:3000/callback"))
            .postLogoutRedirectUris(Set.of("http://localhost:3000/"))
            .scopes(Set.of("openid", "profile", "email"))
            .requireAuthorizationConsent(false)
            .requirePkce(false)
            .build();

    assertEquals("test-client", client.getClientId());
    assertEquals("secret", client.getClientSecret());
    assertEquals("Test Client", client.getClientName());
    assertFalse(client.isPublic());
  }

  @Test
  void shouldDetectPublicClient() {
    OAuthClient publicClient =
        OAuthClient.builder()
            .clientId("public-client")
            .clientSecret(null)
            .clientName("Public Client")
            .clientAuthenticationMethods(Set.of("none"))
            .authorizationGrantTypes(Set.of("authorization_code"))
            .redirectUris(Set.of("http://localhost:3000/callback"))
            .scopes(Set.of("openid"))
            .requirePkce(true)
            .build();

    assertTrue(publicClient.isPublic());
  }

  @Test
  void shouldDetectConfidentialClient() {
    OAuthClient confidentialClient =
        OAuthClient.builder().clientId("confidential-client").clientSecret("secret").build();

    assertFalse(confidentialClient.isPublic());
  }

  @Test
  void shouldValidateGrantType() {
    OAuthClient client =
        OAuthClient.builder()
            .clientId("test-client")
            .authorizationGrantTypes(Set.of("authorization_code", "refresh_token"))
            .build();

    assertTrue(client.supportsGrantType("authorization_code"));
    assertTrue(client.supportsGrantType("refresh_token"));
    assertFalse(client.supportsGrantType("client_credentials"));
  }

  @Test
  void shouldValidateRedirectUri() {
    OAuthClient client =
        OAuthClient.builder()
            .clientId("test-client")
            .redirectUris(
                Set.of("http://localhost:3000/callback", "http://localhost:3000/silent-renew"))
            .build();

    assertTrue(client.isValidRedirectUri("http://localhost:3000/callback"));
    assertTrue(client.isValidRedirectUri("http://localhost:3000/silent-renew"));
    assertFalse(client.isValidRedirectUri("http://malicious.com/callback"));
  }

  @Test
  void shouldValidateScope() {
    OAuthClient client =
        OAuthClient.builder()
            .clientId("test-client")
            .scopes(Set.of("openid", "profile", "email"))
            .build();

    assertTrue(client.isAllowedScope("openid"));
    assertTrue(client.isAllowedScope("profile"));
    assertTrue(client.isAllowedScope("email"));
    assertFalse(client.isAllowedScope("admin"));
  }
}
