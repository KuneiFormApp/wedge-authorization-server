package com.kuneiform.domain.port;

import java.util.List;

/**
 * Port for revoking OAuth2 authorizations (tokens). Bridges the application layer to the
 * infrastructure's OAuth2AuthorizationService.
 */
public interface AuthorizationRevocationPort {

  /**
   * Revoke a specific authorization by its ID.
   *
   * @param authorizationId The authorization ID to revoke
   */
  void revokeById(String authorizationId);

  /**
   * Revoke all authorizations for a user and specific client.
   *
   * @param userId The user ID
   * @param clientId The client ID
   * @return Number of authorizations revoked
   */
  int revokeByUserAndClient(String userId, String clientId);

  /**
   * Revoke all authorizations for a user.
   *
   * @param userId The user ID
   * @return Number of authorizations revoked
   */
  int revokeByUserId(String userId);

  /**
   * Get all authorization IDs for a user.
   *
   * @param userId The user ID
   * @return List of authorization IDs
   */
  List<String> findAuthorizationIdsByUserId(String userId);
}
