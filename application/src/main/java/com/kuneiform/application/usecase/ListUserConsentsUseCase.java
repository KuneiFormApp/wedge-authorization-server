package com.kuneiform.application.usecase;

import com.kuneiform.domain.model.UserConsent;
import com.kuneiform.domain.port.ConsentStoragePort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Use case for listing all consents a user has granted to OAuth clients. Returns domain models for
 * use in both web UI and REST API.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ListUserConsentsUseCase {

  private final ConsentStoragePort consentStorage;

  /**
   * Execute the use case to list all consents for a user.
   *
   * @param userId The user's ID
   * @return List of user consents with client metadata
   */
  public List<UserConsent> execute(String userId) {
    log.debug("Listing consents for user: {}", userId);

    List<UserConsent> consents = consentStorage.findByUserId(userId);

    log.info("Found {} consents for user: {}", consents.size(), userId);
    return consents;
  }
}
