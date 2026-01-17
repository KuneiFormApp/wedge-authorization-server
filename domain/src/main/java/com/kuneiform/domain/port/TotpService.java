package com.kuneiform.domain.port;

/**
 * Port interface for Time-based One-Time Password (TOTP) operations.
 *
 * <p>Defines the contract for generating and validating TOTP codes according to RFC 6238.
 */
public interface TotpService {

  /** Generates a cryptographically secure random Base32-encoded secret for TOTP. */
  String generateSecret();

  /**
   * Validates a TOTP code against a secret. Returns true if valid (including time skew tolerance).
   */
  boolean validateCode(String secret, String code);
}
