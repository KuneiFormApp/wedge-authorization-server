package com.kuneiform.application.usecase;

import com.kuneiform.domain.port.TotpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Use case for validating a TOTP code against a secret.
 *
 * <p>This is used both during MFA setup (to verify the user scanned the QR code correctly) and
 * during login (to verify the user's authenticator app).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ValidateMfaTotpUseCase {

  private final TotpService totpService;

  public boolean execute(String secret, String code) {
    log.debug("Validating TOTP code for MFA");

    if (secret == null || secret.isBlank()) {
      log.warn("Cannot validate TOTP: secret is null or empty");
      return false;
    }

    if (code == null || code.isBlank()) {
      log.warn("Cannot validate TOTP: code is null or empty");
      return false;
    }

    boolean isValid = totpService.validateCode(secret, code);

    if (isValid) {
      log.info("TOTP code validated successfully");
    } else {
      log.warn("TOTP code validation failed");
    }

    return isValid;
  }
}
