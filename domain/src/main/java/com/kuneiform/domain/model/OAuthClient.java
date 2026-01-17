package com.kuneiform.domain.model;

import java.util.Set;
import lombok.Builder;
import lombok.Value;

/**
 * Domain model representing an OAuth 2.1 / OIDC client. Supports both public (PKCE-required) and
 * confidential clients.
 */
@Value
@Builder
public class OAuthClient {
  Long id; // Optional database primary key (null for YAML-based clients)
  String clientId; // OAuth client_id (unique business identifier)
  String clientSecret; // Null for public clients
  String clientName;

  Set<String>
      clientAuthenticationMethods; // e.g., "none", "client_secret_basic", "client_secret_post"
  Set<String>
      authorizationGrantTypes; // e.g., "authorization_code", "refresh_token", "client_credentials"
  Set<String> redirectUris;
  Set<String> postLogoutRedirectUris;
  Set<String> scopes;

  boolean requireAuthorizationConsent;
  boolean requirePkce;

  String tenantId; // Reference to tenant which contains user provider configuration

  // Optional metadata for account page display
  String imageUrl; // URL to client application's logo
  String accessUrl; // URL where user can initiate login (e.g., https://myapp.com/login)

  // Checks if this is a public client (no client secret).
  public boolean isPublic() {
    return clientSecret == null || clientSecret.isBlank();
  }

  // Checks if this client is allowed to use a specific grant type.
  public boolean supportsGrantType(String grantType) {
    return authorizationGrantTypes != null && authorizationGrantTypes.contains(grantType);
  }

  // Checks if a redirect URI is valid for this client.
  public boolean isValidRedirectUri(String redirectUri) {
    return redirectUris != null && redirectUris.contains(redirectUri);
  }

  // Checks if the client is allowed to request a specific scope.
  public boolean isAllowedScope(String scope) {
    return scopes != null && scopes.contains(scope);
  }
}
