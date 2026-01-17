package com.kuneiform.domain.port;

import com.kuneiform.domain.model.UserConsent;
import java.util.List;

/**
 * Port for managing OAuth2 authorization consents. Tracks which clients a user has authorized and
 * what scopes they granted.
 */
public interface ConsentStoragePort {

  /**
   * Find all consents for a given user.
   *
   * @param userId The user's ID (principal name)
   * @return List of user consents with client metadata
   */
  List<UserConsent> findByUserId(String userId);

  /**
   * Revoke all consents for a specific user-client pair. This removes the user's authorization for
   * the client.
   *
   * @param userId The user's ID
   * @param clientId The OAuth client ID
   */
  void revokeByClientId(String userId, String clientId);

  /**
   * Check if a user has an active consent for a specific client.
   *
   * @param userId The user's ID
   * @param clientId The OAuth client ID
   * @return true if consent exists
   */
  boolean hasConsent(String userId, String clientId);
}
