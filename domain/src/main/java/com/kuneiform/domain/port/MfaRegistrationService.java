package com.kuneiform.domain.port;

/**
 * Port interface for registering MFA configuration with external user provider.
 *
 * <p>Defines the contract for sending MFA registration data to the user provider backend after
 * successful MFA setup.
 */
public interface MfaRegistrationService {

  /** Registers MFA configuration with the user provider. */
  boolean registerMfa(String clientId, String userId, String mfaSecret, String mfaKeyId);
}
