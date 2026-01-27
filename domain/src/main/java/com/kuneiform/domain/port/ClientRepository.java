package com.kuneiform.domain.port;

import com.kuneiform.domain.model.OAuthClient;
import java.util.List;
import java.util.Optional;

/**
 * Port interface for OAuth client storage and retrieval. Can be implemented with static
 * configuration or database persistence.
 */
public interface ClientRepository {

  Optional<OAuthClient> findByClientId(String clientId);

  /**
   * Validates a client's credentials. For public clients, only validates the client ID exists. For
   * confidential clients, validates both client ID and secret.
   */
  boolean validateClient(String clientId, String clientSecret);

  /** Only supported for database-backed implementations. */
  default List<OAuthClient> findAll() {
    throw new UnsupportedOperationException("findAll() not supported for YAML storage");
  }

  /** Only supported for database-backed implementations. */
  default OAuthClient save(OAuthClient client) {
    throw new UnsupportedOperationException("save() not supported for YAML storage");
  }

  /** Only supported for database-backed implementations. */
  default void deleteByClientId(String clientId) {
    throw new UnsupportedOperationException("deleteByClientId() not supported for YAML storage");
  }
}
