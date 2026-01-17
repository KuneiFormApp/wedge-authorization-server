package com.kuneiform.application.usecase;

import com.kuneiform.domain.port.SessionStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Use case for revoking a specific user session. Can be called from both web UI and REST API. */
@Slf4j
@Service
@RequiredArgsConstructor
public class RevokeSessionUseCase {

  private final SessionStorage sessionStorage;

  /**
   * Execute the use case to revoke a specific session.
   *
   * @param sessionId The session ID (authorization code)
   * @param userId The user's ID (for security verification)
   * @throws IllegalArgumentException if session doesn't exist or doesn't belong to user
   */
  public void execute(String sessionId, String userId) {
    log.info("Revoking session: {} for user: {}", sessionId, userId);

    // Verify session exists and belongs to user
    var session = sessionStorage.findByAuthorizationCode(sessionId);

    if (session.isEmpty()) {
      log.warn("Attempted to revoke non-existent session: {}", sessionId);
      throw new IllegalArgumentException("Session not found");
    }

    if (!session.get().getUserId().equals(userId)) {
      log.warn(
          "User {} attempted to revoke session {} belonging to another user", userId, sessionId);
      throw new IllegalArgumentException("Session does not belong to user");
    }

    sessionStorage.deleteByAuthorizationCode(sessionId);

    log.info("Successfully revoked session: {} for user: {}", sessionId, userId);
  }
}
