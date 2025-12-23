package com.kuneiform.domain.port;

import com.kuneiform.domain.model.OAuthClient;
import java.util.Optional;

/**
 * Port interface for OAuth client storage and retrieval. Can be implemented with static
 * configuration or database persistence.
 */
public interface ClientRepository {

  /**
   * Finds an OAuth client by its client ID.
   *
   * @param clientId the client ID to search for
   * @return Optional containing the OAuthClient if found, empty otherwise
   */
  Optional<OAuthClient> findByClientId(String clientId);

  /**
   * Validates a client's credentials. For public clients, only validates the client ID exists. For
   * confidential clients, validates both client ID and secret.
   *
   * @param clientId the client ID
   * @param clientSecret the client secret (can be null for public clients)
   * @return true if the client is valid, false otherwise
   */
  boolean validateClient(String clientId, String clientSecret);
}
