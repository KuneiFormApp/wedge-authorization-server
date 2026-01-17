package com.kuneiform.application.usecase;

import com.kuneiform.domain.port.TotpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Use case for generating a new TOTP secret for MFA setup.
 *
 * <p>This secret will be displayed to the user as a QR code and stored in the user provider after
 * successful verification.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GenerateMfaSecretUseCase {

  private final TotpService totpService;

  /**
   * Executes the use case to generate a new MFA secret.
   *
   * @return a Base32-encoded TOTP secret
   */
  public String execute() {
    log.debug("Generating new MFA secret");
    String secret = totpService.generateSecret();
    log.info("Generated new MFA secret (length: {})", secret.length());
    return secret;
  }
}
