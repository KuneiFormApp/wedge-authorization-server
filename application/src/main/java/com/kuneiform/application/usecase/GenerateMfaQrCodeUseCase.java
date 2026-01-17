package com.kuneiform.application.usecase;

import com.kuneiform.domain.port.QrCodeService;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Use case for generating QR codes for TOTP MFA registration.
 *
 * <p>Generates a QR code image containing an otpauth:// URI that can be scanned by authenticator
 * apps.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GenerateMfaQrCodeUseCase {

  private final QrCodeService qrCodeService;

  /**
   * Executes the use case to generate a QR code for MFA setup.
   *
   * @param issuer the application/issuer name (e.g., "WedgeAuth")
   * @param accountName the user's account name (typically username or email)
   * @param secret the Base32-encoded TOTP secret
   * @param width QR code image width in pixels
   * @param height QR code image height in pixels
   * @return PNG image data as byte array
   * @throws QrCodeService.QrCodeGenerationException if QR code generation fails
   */
  public byte[] execute(String issuer, String accountName, String secret, int width, int height)
      throws QrCodeService.QrCodeGenerationException {

    log.debug(
        "Generating QR code for MFA: issuer={}, account={}, size={}x{}",
        issuer,
        accountName,
        width,
        height);

    // Build otpauth:// URI for TOTP
    // Format: otpauth://totp/IssuerName:AccountName?secret=SECRET&issuer=IssuerName
    String otpauthUri = buildOtpauthUri(issuer, accountName, secret);

    log.debug("Generated otpauth URI (length: {})", otpauthUri.length());

    byte[] qrCodeBytes = qrCodeService.generateQrCode(otpauthUri, width, height);

    log.info(
        "Generated QR code for MFA: issuer={}, account={}, size={}x{}, bytes={}",
        issuer,
        accountName,
        width,
        height,
        qrCodeBytes.length);

    return qrCodeBytes;
  }

  /**
   * Builds the otpauth:// URI for TOTP.
   *
   * @param issuer the issuer name
   * @param accountName the account name
   * @param secret the TOTP secret
   * @return the otpauth:// URI string
   */
  private String buildOtpauthUri(String issuer, String accountName, String secret) {
    try {
      String encodedIssuer = URLEncoder.encode(issuer, StandardCharsets.UTF_8.toString());
      String encodedAccountName = URLEncoder.encode(accountName, StandardCharsets.UTF_8.toString());
      String encodedSecret = URLEncoder.encode(secret, StandardCharsets.UTF_8.toString());

      return String.format(
          "otpauth://totp/%s:%s?secret=%s&issuer=%s",
          encodedIssuer, encodedAccountName, encodedSecret, encodedIssuer);
    } catch (UnsupportedEncodingException e) {
      // This should never happen with UTF-8
      log.error("Failed to encode otpauth URI", e);
      throw new RuntimeException("Failed to encode otpauth URI", e);
    }
  }
}
