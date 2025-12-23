package com.kuneiform.infraestructure.security;

import com.kuneiform.domain.model.User;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.stereotype.Component;

/**
 * Customizes JWT tokens (access_token and id_token) by mapping User domain model fields into JWT
 * claims. This ensures that user information from the external user provider is properly included
 * in the issued tokens.
 */
@Slf4j
@Component
public class WedgeTokenCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {

  @Override
  public void customize(JwtEncodingContext context) {
    Authentication principal = context.getPrincipal();

    // Extract the User object from the authentication principal
    if (!(principal.getPrincipal() instanceof User)) {
      log.warn(
          "Principal is not a User object, skipping token customization. Principal type: {}",
          principal.getPrincipal().getClass().getName());
      return;
    }

    User user = (User) principal.getPrincipal();

    log.debug(
        "Customizing token for user: {} (userId: {}), token type: {}",
        user.getUsername(),
        user.getUserId(),
        context.getTokenType().getValue());

    // Customize claims for both access_token and id_token
    context
        .getClaims()
        .claims(
            claims -> {
              // Override 'sub' claim with userId instead of username
              claims.put("sub", user.getUserId());

              // Add email claim
              if (user.getEmail() != null) {
                claims.put("email", user.getEmail());
              }

              // Add username as a separate claim (since sub is now userId)
              claims.put("username", user.getUsername());

              // Map all metadata fields directly into the token payload
              if (user.getMetadata() != null && !user.getMetadata().isEmpty()) {
                for (Map.Entry<String, Object> entry : user.getMetadata().entrySet()) {
                  String key = entry.getKey();
                  Object value = entry.getValue();

                  // Add metadata to token claims
                  claims.put(key, value);

                  log.trace("Added metadata claim: {} = {}", key, value);
                }
              }
            });

    // Additional customization specific to ID tokens
    if (OidcParameterNames.ID_TOKEN.equals(context.getTokenType().getValue())) {
      log.debug("Applied ID token customizations for user: {}", user.getUsername());
    }
  }
}
