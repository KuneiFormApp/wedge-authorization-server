package com.kuneiform.infrastructure.security;

import com.kuneiform.infrastructure.adapter.InMemoryOAuth2AuthorizationServiceAdapter;
import com.kuneiform.infrastructure.adapter.RedisOAuth2AuthorizationServiceAdapter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

/**
 * Custom logout handler that revokes OAuth2 authorizations when a user logs out.
 *
 * <p>This handler is invoked during logout to remove all OAuth2 authorization records associated
 * with the authenticated user, effectively invalidating their refresh tokens and preventing new
 * access tokens from being issued.
 *
 * <p><b>Important:</b> JWT access tokens that have already been issued will remain valid until
 * their natural expiration. This is acceptable for most applications when using short-lived access
 * tokens (3-5 minutes recommended for production).
 *
 * <p>This handler works with both in-memory and Redis-backed {@link OAuth2AuthorizationService}
 * implementations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthorizationRevocationLogoutHandler implements LogoutHandler {

  private final OAuth2AuthorizationService authorizationService;

  @Override
  public void logout(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication) {

    if (authentication == null || authentication.getPrincipal() == null) {
      log.debug("No authentication present, skipping OAuth2 authorization revocation");
      return;
    }

    // Extract userId from the User principal
    String userId;
    Object principal = authentication.getPrincipal();

    if (principal instanceof com.kuneiform.domain.model.User) {
      userId = ((com.kuneiform.domain.model.User) principal).getUserId();
    } else {
      // Fallback to getName() if principal is not a User object
      log.warn(
          "Principal is not a User object, using getName() as fallback: {}",
          principal.getClass().getName());
      userId = authentication.getName();
    }

    if (userId == null || userId.isBlank()) {
      log.warn("No userId found in authentication, skipping OAuth2 authorization revocation");
      return;
    }

    log.info("Revoking OAuth2 authorizations for userId: {}", userId);

    int revokedCount = 0;

    // Find and revoke all authorizations for this user
    // Note: We loop to handle multiple authorizations per user (e.g., multiple
    // devices/clients)
    OAuth2Authorization authorization = findAuthorizationByUserId(userId);

    while (authorization != null) {
      log.debug(
          "Revoking authorization: id={}, clientId={}",
          authorization.getId(),
          authorization.getRegisteredClientId());

      authorizationService.remove(authorization);
      revokedCount++;

      // Check if there are more authorizations for this user
      authorization = findAuthorizationByUserId(userId);
    }

    if (revokedCount > 0) {
      log.info(
          "Successfully revoked {} OAuth2 authorization(s) for userId: {}", revokedCount, userId);
    } else {
      log.debug("No OAuth2 authorizations found for userId: {}", userId);
    }
  }

  /** Uses the custom findByUserId method available in our authorization service adapters. */
  private OAuth2Authorization findAuthorizationByUserId(String userId) {
    if (authorizationService instanceof InMemoryOAuth2AuthorizationServiceAdapter) {
      return ((InMemoryOAuth2AuthorizationServiceAdapter) authorizationService)
          .findByUserId(userId);
    } else if (authorizationService instanceof RedisOAuth2AuthorizationServiceAdapter) {
      return ((RedisOAuth2AuthorizationServiceAdapter) authorizationService).findByUserId(userId);
    }

    log.warn(
        "OAuth2AuthorizationService implementation does not support findByUserId. "
            + "Logout will clear HTTP session but not revoke OAuth2 tokens.");
    return null;
  }
}
