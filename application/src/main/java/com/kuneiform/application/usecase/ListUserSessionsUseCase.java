package com.kuneiform.application.usecase;

import com.kuneiform.domain.model.AuthorizationSession;
import com.kuneiform.domain.port.SessionStorage;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Use case for listing all active sessions for a user. Returns domain models for use in both web UI
 * and REST API.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ListUserSessionsUseCase {

  private final SessionStorage sessionStorage;

  /**
   * Execute the use case to list all sessions for a user.
   *
   * @param userId The user's ID
   * @return List of authorization sessions
   */
  public List<AuthorizationSession> execute(String userId) {
    log.debug("Listing sessions for user: {}", userId);

    List<AuthorizationSession> sessions = sessionStorage.findByUserId(userId);

    log.info("Found {} sessions for user: {}", sessions.size(), userId);
    return sessions;
  }
}
