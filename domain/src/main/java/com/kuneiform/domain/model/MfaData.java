package com.kuneiform.domain.model;

import java.io.Serializable;
import lombok.Builder;
import lombok.Value;

/**
 * Domain model representing Multi-Factor Authentication data for a user.
 *
 * <p>Contains the TOTP secret and registration status for MFA.
 */
@Value
@Builder
public class MfaData implements Serializable {
  private static final long serialVersionUID = 1L;

  /**
   * Whether the user has completed MFA registration (scanned QR code and verified initial TOTP
   * code).
   */
  boolean twoFaRegistered;

  /**
   * Identifier for the MFA key shown in authenticator apps. Format: "IssuerName:username" (e.g.,
   * "WedgeAuth:user@example.com").
   */
  String mfaKeyId;

  /**
   * Base32-encoded TOTP secret used for generating time-based one-time passwords. This is sensitive
   * cryptographic material and should be stored encrypted.
   */
  String mfaSecret;
}
