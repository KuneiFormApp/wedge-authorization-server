package com.kuneiform.infraestructure.util;

import java.security.GeneralSecurityException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Implementation of TOTP (Time-Based One-Time Password) Algorithm as defined in RFC 6238.
 *
 * <p>This utility class provides methods for generating TOTP codes using HMAC-SHA1. It is
 * compatible with standard authenticator apps including Google Authenticator, Microsoft
 * Authenticator, Authy, 1Password, etc.
 */
public final class TotpAlgorithm {

  private static final int TIME_STEP_SECONDS = 30;
  private static final String HMAC_ALGORITHM = "HmacSHA1";

  private TotpAlgorithm() {
    // Utility class - prevent instantiation
  }

  /**
   * Generates a TOTP code for the current time.
   *
   * @throws GeneralSecurityException if HMAC-SHA1 is not available
   */
  public static String generateOTP(byte[] secretKey) throws GeneralSecurityException {
    long currentTimeStep = System.currentTimeMillis() / 1000L / TIME_STEP_SECONDS;
    return generateOTP(secretKey, currentTimeStep);
  }

  /**
   * Generates a TOTP code for a specific time step.
   *
   * @throws GeneralSecurityException if HMAC-SHA1 is not available
   */
  public static String generateOTP(byte[] secretKey, long timeStep)
      throws GeneralSecurityException {
    // Convert time step to byte array (8 bytes, big-endian)
    byte[] timeBytes = longToBytes(timeStep);

    // Perform HMAC-SHA1
    byte[] hash = hmacSha1(secretKey, timeBytes);

    // Dynamic truncation (extract 4 bytes from hash)
    int offset = hash[hash.length - 1] & 0xf;
    int binary =
        ((hash[offset] & 0x7f) << 24)
            | ((hash[offset + 1] & 0xff) << 16)
            | ((hash[offset + 2] & 0xff) << 8)
            | (hash[offset + 3] & 0xff);

    // Get 6-digit OTP
    int otp = binary % 1_000_000;
    return String.format("%06d", otp);
  }

  private static byte[] longToBytes(long value) {
    byte[] result = new byte[8];
    for (int i = 7; i >= 0; i--) {
      result[i] = (byte) (value & 0xff);
      value >>= 8;
    }
    return result;
  }

  private static byte[] hmacSha1(byte[] key, byte[] data) throws GeneralSecurityException {
    Mac hmac = Mac.getInstance(HMAC_ALGORITHM);
    SecretKeySpec keySpec = new SecretKeySpec(key, "RAW");
    hmac.init(keySpec);
    return hmac.doFinal(data);
  }
}
