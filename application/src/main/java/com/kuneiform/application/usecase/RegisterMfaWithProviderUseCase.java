package com.kuneiform.application.usecase;

import com.kuneiform.domain.port.MfaRegistrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Use case for registering MFA configuration with the external user provider.
 *
 * <p>After successful MFA setup and verification, this use case sends the MFA data to the user
 * provider backend for persistent storage.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegisterMfaWithProviderUseCase {

  private final MfaRegistrationService mfaRegistrationService;

  /**
   * Executes the use case to register MFA data with the user provider.
   *
   * @param clientId the client ID to resolve the user provider
   * @param userId the user ID
   * @param mfaSecret the TOTP secret to store
   * @param mfaKeyId the MFA key identifier
   * @return true if registration was successful, false otherwise
   */
  public boolean execute(String clientId, String userId, String mfaSecret, String mfaKeyId) {

    log.debug("Executing MFA registration for user: {} via client: {}", userId, clientId);

    return mfaRegistrationService.registerMfa(clientId, userId, mfaSecret, mfaKeyId);
  }
}
