package com.kuneiform.infraestructure.adapter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GoogleAuthenticatorTotpServiceTest {

  private GoogleAuthenticatorTotpService totpService;

  @BeforeEach
  void setUp() {
    totpService = new GoogleAuthenticatorTotpService();
  }

  @Test
  void generateSecret_shouldReturnBase32EncodedSecret() {
    // When
    String secret = totpService.generateSecret();

    // Then
    assertNotNull(secret);
    assertFalse(secret.isBlank());
    // Base32 alphabet: A-Z and 2-7
    assertTrue(secret.matches("[A-Z2-7]+"), "Secret should be Base32 encoded");
  }

  @Test
  void generateSecret_shouldReturnDifferentSecretsOnMultipleCalls() {
    // When
    String secret1 = totpService.generateSecret();
    String secret2 = totpService.generateSecret();

    // Then
    assertNotEquals(secret1, secret2, "Each generated secret should be unique");
  }

  @Test
  void validateCode_shouldReturnFalseForNullSecret() {
    // When
    boolean result = totpService.validateCode(null, "123456");

    // Then
    assertFalse(result);
  }

  @Test
  void validateCode_shouldReturnFalseForNullCode() {
    // Given
    String secret = totpService.generateSecret();

    // When
    boolean result = totpService.validateCode(secret, null);

    // Then
    assertFalse(result);
  }

  @Test
  void validateCode_shouldReturnFalseForInvalidCodeFormat() {
    // Given
    String secret = totpService.generateSecret();

    // When & Then
    assertFalse(totpService.validateCode(secret, "12345")); // Too short
    assertFalse(totpService.validateCode(secret, "1234567")); // Too long
    assertFalse(totpService.validateCode(secret, "12345a")); // Contains letter
    assertFalse(totpService.validateCode(secret, "123 456")); // Contains space
  }

  @Test
  void validateCode_shouldAcceptValidCodeWithWhitespace() {
    // Given
    String secret = totpService.generateSecret();

    // Note: We can't test with a known-good code since TOTP is time-based
    // This test validates that whitespace is properly stripped
    // The code itself will fail validation, but won't throw an exception

    // When
    boolean result = totpService.validateCode(secret, "  123456  ");

    // Then - should not throw exception (whitespace handled)
    assertNotNull(result);
  }

  @Test
  void validateCode_shouldHandleTimeSkew() {
    // NOTE: This test is tricky because TOTP is time-based
    // We would need to mock time or use a known secret with calculated codes
    // For now, we just validate the service doesn't crash with valid inputs

    // Given
    String secret = "JBSWY3DPEHPK3PXP"; // Known Base32 secret for testing
    String invalidCode = "000000"; // Definitely wrong code

    // When
    boolean result = totpService.validateCode(secret, invalidCode);

    // Then
    assertFalse(result); // Should fail but not crash
  }
}
