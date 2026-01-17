package com.kuneiform.infraestructure.adapter;

import static org.junit.jupiter.api.Assertions.*;

import com.kuneiform.domain.port.QrCodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ZxingQrCodeServiceTest {

  private ZxingQrCodeService qrCodeService;

  @BeforeEach
  void setUp() {
    qrCodeService = new ZxingQrCodeService();
  }

  @Test
  void generateQrCode_shouldReturnPngImageBytes() throws QrCodeService.QrCodeGenerationException {
    // Given
    String data =
        "otpauth://totp/WedgeAuth:user@example.com?secret=JBSWY3DPEHPK3PXP&issuer=WedgeAuth";
    int width = 300;
    int height = 300;

    // When
    byte[] qrCodeBytes = qrCodeService.generateQrCode(data, width, height);

    // Then
    assertNotNull(qrCodeBytes);
    assertTrue(qrCodeBytes.length > 0, "QR code should contain data");

    // PNG signature: starts with \u0089PNG
    assertEquals((byte) 0x89, qrCodeBytes[0], "PNG signature byte 1");
    assertEquals((byte) 'P', qrCodeBytes[1], "PNG signature byte 2");
    assertEquals((byte) 'N', qrCodeBytes[2], "PNG signature byte 3");
    assertEquals((byte) 'G', qrCodeBytes[3], "PNG signature byte 4");
  }

  @Test
  void generateQrCode_shouldHandleDifferentSizes() throws QrCodeService.QrCodeGenerationException {
    // Given
    String data = "otpauth://totp/Test:user@test.com?secret=SECRET&issuer=Test";

    // When
    byte[] small = qrCodeService.generateQrCode(data, 100, 100);
    byte[] large = qrCodeService.generateQrCode(data, 500, 500);

    // Then
    assertNotNull(small);
    assertNotNull(large);
    assertTrue(small.length > 0);
    assertTrue(large.length > 0);
    // Larger QR codes typically have more bytes
    assertTrue(large.length > small.length);
  }

  @Test
  void generateQrCode_shouldThrowExceptionForNullData() {
    // When & Then
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          qrCodeService.generateQrCode(null, 300, 300);
        });
  }

  @Test
  void generateQrCode_shouldThrowExceptionForInvalidDimensions() {
    // Given
    String data = "test-data";

    // When & Then
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          qrCodeService.generateQrCode(data, 0, 300);
        });

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          qrCodeService.generateQrCode(data, 300, 0);
        });

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          qrCodeService.generateQrCode(data, -100, 300);
        });
  }

  @Test
  void generateQrCode_shouldEncodeComplexOtpauthUri()
      throws QrCodeService.QrCodeGenerationException {
    // Given - Complex OTPAUTH URI with special characters
    String complexData =
        "otpauth://totp/WedgeAuth:user%2Btest@example.com?"
            + "secret=JBSWY3DPEHPK3PXP&issuer=WedgeAuth&digits=6&period=30";
    int size = 250;

    // When
    byte[] qrCode = qrCodeService.generateQrCode(complexData, size, size);

    // Then
    assertNotNull(qrCode);
    assertTrue(qrCode.length > 0);
    // Verify it's a valid PNG
    assertEquals((byte) 0x89, qrCode[0]);
  }
}
