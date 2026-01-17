package com.kuneiform.domain.port;

import com.kuneiform.domain.model.AuthorizationSession;
import java.util.List;
import java.util.Optional;

/**
 * Port interface for authorization session storage. Can be implemented with in-memory cache or
 * Redis.
 */
public interface SessionStorage {

  void save(AuthorizationSession session);

  Optional<AuthorizationSession> findByAuthorizationCode(String authorizationCode);

  /**
   * Find all sessions for a specific user.
   *
   * @param userId The user's ID
   * @return List of authorization sessions for the user
   */
  List<AuthorizationSession> findByUserId(String userId);

  void deleteByAuthorizationCode(String authorizationCode);

  // Deletes all expired sessions. This can be used for cleanup tasks.
  void deleteExpiredSessions();
}
