package com.kuneiform.domain.port;

/**
 * Port interface for Time-based One-Time Password (TOTP) operations.
 *
 * <p>Defines the contract for generating and validating TOTP codes according to RFC 6238.
 */
public interface TotpService {

  /**
   * Generates a cryptographically secure random Base32-encoded secret for TOTP.
   *
   * @return a Base32-encoded secret string (typically 32 characters)
   */
  String generateSecret();

  /**
   * Validates a TOTP code against a secret.
   *
   * @param secret the Base32-encoded TOTP secret
   * @param code the 6-digit TOTP code provided by the user
   * @return true if the code is valid (including time skew tolerance), false otherwise
   */
  boolean validateCode(String secret, String code);
}
