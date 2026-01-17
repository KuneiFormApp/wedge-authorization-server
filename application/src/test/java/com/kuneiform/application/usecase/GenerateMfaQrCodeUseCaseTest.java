package com.kuneiform.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.kuneiform.domain.port.QrCodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GenerateMfaQrCodeUseCaseTest {

  @Mock private QrCodeService qrCodeService;

  private GenerateMfaQrCodeUseCase useCase;

  @BeforeEach
  void setUp() {
    useCase = new GenerateMfaQrCodeUseCase(qrCodeService);
  }

  @Test
  void execute_shouldGenerateOtpauthUriAndQrCode() throws QrCodeService.QrCodeGenerationException {
    // Given
    String issuer = "WedgeAuth";
    String accountName = "user@example.com";
    String secret = "JBSWY3DPEHPK3PXP";
    int width = 300;
    int height = 300;

    byte[] expectedQrCode = new byte[] {1, 2, 3, 4, 5};
    String expectedUri =
        "otpauth://totp/WedgeAuth:user%40example.com?secret=JBSWY3DPEHPK3PXP&issuer=WedgeAuth";

    when(qrCodeService.generateQrCode(expectedUri, width, height)).thenReturn(expectedQrCode);

    // When
    byte[] result = useCase.execute(issuer, accountName, secret, width, height);

    // Then
    assertArrayEquals(expectedQrCode, result);
    verify(qrCodeService).generateQrCode(expectedUri, width, height);
  }

  @Test
  void execute_shouldHandleSpecialCharactersInAccountName()
      throws QrCodeService.QrCodeGenerationException {
    // Given
    String issuer = "WedgeAuth";
    String accountName = "user+test@example.com";
    String secret = "SECRET123";
    int size = 250;

    byte[] qrCode = new byte[] {10, 20, 30};
    when(qrCodeService.generateQrCode(anyString(), eq(size), eq(size))).thenReturn(qrCode);

    // When
    byte[] result = useCase.execute(issuer, accountName, secret, size, size);

    // Then
    assertNotNull(result);
    verify(qrCodeService)
        .generateQrCode(
            argThat(
                uri ->
                    uri.contains("WedgeAuth")
                        && uri.contains(secret)
                        && uri.startsWith("otpauth://totp/")),
            eq(size),
            eq(size));
  }

  @Test
  void execute_shouldIncludeIssuerInUri() throws QrCodeService.QrCodeGenerationException {
    // Given
    String issuer = "TestIssuer";
    String accountName = "test@test.com";
    String secret = "TESTSECRET";
    int size = 200;

    byte[] qrCode = new byte[] {5, 10};
    when(qrCodeService.generateQrCode(anyString(), eq(size), eq(size))).thenReturn(qrCode);

    // When
    useCase.execute(issuer, accountName, secret, size, size);

    // Then
    verify(qrCodeService)
        .generateQrCode(argThat(uri -> uri.contains("issuer=" + issuer)), eq(size), eq(size));
  }
}
