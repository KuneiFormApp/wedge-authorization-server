package com.kuneiform.application.usecase;

import com.kuneiform.domain.port.AuthorizationRevocationPort;
import com.kuneiform.domain.port.ConsentStoragePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Use case for revoking a user's consent for a specific OAuth client. Removes all authorization
 * grants for the user-client pair and revokes active tokens. Can be called from both web UI and
 * REST API.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RevokeConsentUseCase {

  private final ConsentStoragePort consentStorage;
  private final AuthorizationRevocationPort authorizationRevocation;

  /**
   * Execute the use case to revoke consent for a specific client.
   *
   * @param userId The user's ID
   * @param clientId The OAuth client ID
   * @throws IllegalArgumentException if consent doesn't exist
   */
  public void execute(String userId, String clientId) {
    log.info("Revoking consent for user: {} and client: {}", userId, clientId);

    // Verify consent exists before revoking
    if (!consentStorage.hasConsent(userId, clientId)) {
      log.warn(
          "Attempted to revoke non-existent consent for user: {} and client: {}", userId, clientId);
      throw new IllegalArgumentException("No consent found for user and client");
    }

    // Revoke all OAuth2 authorizations (tokens) for this user-client pair
    int revokedTokens = authorizationRevocation.revokeByUserAndClient(userId, clientId);
    log.info(
        "Revoked {} OAuth2 authorizations for user: {} and client: {}",
        revokedTokens,
        userId,
        clientId);

    // Remove consent record
    consentStorage.revokeByClientId(userId, clientId);

    log.info("Successfully revoked consent for user: {} and client: {}", userId, clientId);
  }
}
