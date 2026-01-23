package com.kuneiform.infrastructure.adapter;

import com.kuneiform.domain.port.AuthorizationRevocationPort;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.stereotype.Component;

/**
 * Adapter that bridges AuthorizationRevocationPort to Spring's OAuth2AuthorizationService. Supports
 * both in-memory and Redis-backed authorization services.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthorizationRevocationAdapter implements AuthorizationRevocationPort {

  private final OAuth2AuthorizationService authorizationService;

  @Override
  public void revokeById(String authorizationId) {
    if (authorizationId == null || authorizationId.isBlank()) {
      log.debug("No authorization ID provided, skipping revocation");
      return;
    }

    OAuth2Authorization authorization = authorizationService.findById(authorizationId);
    if (authorization != null) {
      authorizationService.remove(authorization);
      log.info("Revoked authorization: {}", authorizationId);
    } else {
      log.debug("Authorization not found: {}", authorizationId);
    }
  }

  @Override
  public int revokeByUserAndClient(String userId, String clientId) {
    List<OAuth2Authorization> authorizations = findAuthorizationsByUserId(userId);
    int revokedCount = 0;

    for (OAuth2Authorization auth : authorizations) {
      if (auth.getRegisteredClientId().equals(clientId)) {
        authorizationService.remove(auth);
        revokedCount++;
        log.debug(
            "Revoked authorization {} for user {} and client {}", auth.getId(), userId, clientId);
      }
    }

    log.info("Revoked {} authorizations for user {} and client {}", revokedCount, userId, clientId);
    return revokedCount;
  }

  @Override
  public int revokeByUserId(String userId) {
    List<OAuth2Authorization> authorizations = findAuthorizationsByUserId(userId);
    int revokedCount = 0;

    for (OAuth2Authorization auth : authorizations) {
      authorizationService.remove(auth);
      revokedCount++;
    }

    log.info("Revoked {} authorizations for user {}", revokedCount, userId);
    return revokedCount;
  }

  @Override
  public List<String> findAuthorizationIdsByUserId(String userId) {
    return findAuthorizationsByUserId(userId).stream().map(OAuth2Authorization::getId).toList();
  }

  /**
   * Find all authorizations for a user. This method handles different OAuth2AuthorizationService
   * implementations. Note: Current implementations return a single authorization per user.
   */
  private List<OAuth2Authorization> findAuthorizationsByUserId(String userId) {
    List<OAuth2Authorization> result = new ArrayList<>();

    if (authorizationService instanceof InMemoryOAuth2AuthorizationServiceAdapter inMemoryAdapter) {
      OAuth2Authorization auth = inMemoryAdapter.findByUserId(userId);
      if (auth != null) {
        result.add(auth);
      }
    } else if (authorizationService
        instanceof RedisOAuth2AuthorizationServiceAdapter redisAdapter) {
      OAuth2Authorization auth = redisAdapter.findByUserId(userId);
      if (auth != null) {
        result.add(auth);
      }
    } else {
      log.warn(
          "OAuth2AuthorizationService implementation does not support findByUserId. "
              + "User-based revocation not available.");
    }

    return result;
  }
}
