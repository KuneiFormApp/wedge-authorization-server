package com.kuneiform.domain.port;

/**
 * Port interface for registering MFA configuration with external user provider.
 *
 * <p>Defines the contract for sending MFA registration data to the user provider backend after
 * successful MFA setup.
 */
public interface MfaRegistrationService {

  /**
   * Registers MFA configuration with the user provider.
   *
   * @param clientId the client ID to resolve the user provider
   * @param userId the user ID
   * @param mfaSecret the TOTP secret to register
   * @param mfaKeyId the MFA key identifier
   * @return true if registration was successful, false otherwise
   */
  boolean registerMfa(String clientId, String userId, String mfaSecret, String mfaKeyId);
}
