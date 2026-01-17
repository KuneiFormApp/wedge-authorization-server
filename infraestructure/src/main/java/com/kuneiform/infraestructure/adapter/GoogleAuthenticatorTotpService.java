package com.kuneiform.infraestructure.adapter;

import com.kuneiform.domain.port.TotpService;
import com.kuneiform.infraestructure.util.TotpAlgorithm;
import java.security.SecureRandom;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base32;
import org.springframework.stereotype.Component;

/**
 * Implementation of TOTP service using our custom TotpAlgorithm utility.
 *
 * <p>This implementation follows RFC 6238 (TOTP: Time-Based One-Time Password Algorithm) and is
 * compatible with all standard authenticator apps including Google Authenticator, Microsoft
 * Authenticator, Authy, 1Password, etc.
 */
@Slf4j
@Component
public class GoogleAuthenticatorTotpService implements TotpService {

  private static final int SECRET_LENGTH = 20; // 20 bytes = 160 bits for Base32 encoding
  private static final Base32 BASE32 = new Base32();

  @Override
  public String generateSecret() {
    SecureRandom random = new SecureRandom();
    byte[] bytes = new byte[SECRET_LENGTH];
    random.nextBytes(bytes);
    String secret = BASE32.encodeToString(bytes);
    log.debug("Generated TOTP secret (length: {})", secret.length());
    return secret;
  }

  @Override
  public boolean validateCode(String secret, String code) {
    if (secret == null || code == null) {
      log.warn("TOTP validation failed: secret or code is null");
      return false;
    }

    try {
      String cleanCode = code.trim().replaceAll("\\s+", "");

      if (!cleanCode.matches("\\d{6}")) {
        log.warn("Invalid TOTP code format: {}", cleanCode);
        return false;
      }

      byte[] secretBytes = BASE32.decode(secret);
      String actualCode = TotpAlgorithm.generateOTP(secretBytes);

      boolean isValid = actualCode.equals(cleanCode);

      if (!isValid) {
        long currentTime = System.currentTimeMillis() / 1000L;
        isValid =
            checkCode(secret, cleanCode, currentTime - 30)
                || checkCode(secret, cleanCode, currentTime + 30);
      }

      if (isValid) {
        log.debug("TOTP code validated successfully");
      } else {
        log.warn("TOTP code validation failed");
      }

      return isValid;
    } catch (Exception e) {
      log.error("Error validating TOTP code", e);
      return false;
    }
  }

  /**
   * Checks if the provided code matches the expected code at the given time.
   *
   * @param secret the TOTP secret (Base32-encoded)
   * @param code the code to validate
   * @param timeInSeconds the time in seconds since epoch
   * @return true if the code matches
   */
  private boolean checkCode(String secret, String code, long timeInSeconds) {
    try {
      long timeStep = timeInSeconds / 30L;

      byte[] secretBytes = BASE32.decode(secret);
      String expectedCode = TotpAlgorithm.generateOTP(secretBytes, timeStep);
      return expectedCode.equals(code);
    } catch (Exception e) {
      log.error("Error checking TOTP code for time {}", timeInSeconds, e);
      return false;
    }
  }
}
