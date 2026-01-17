package com.kuneiform.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.kuneiform.domain.port.TotpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ValidateMfaTotpUseCaseTest {

  @Mock private TotpService totpService;

  private ValidateMfaTotpUseCase useCase;

  @BeforeEach
  void setUp() {
    useCase = new ValidateMfaTotpUseCase(totpService);
  }

  @Test
  void execute_shouldReturnTrueForValidCode() {
    // Given
    String secret = "JBSWY3DPEHPK3PXP";
    String code = "123456";
    when(totpService.validateCode(secret, code)).thenReturn(true);

    // When
    boolean result = useCase.execute(secret, code);

    // Then
    assertTrue(result);
    verify(totpService, times(1)).validateCode(secret, code);
  }

  @Test
  void execute_shouldReturnFalseForInvalidCode() {
    // Given
    String secret = "JBSWY3DPEHPK3PXP";
    String code = "000000";
    when(totpService.validateCode(secret, code)).thenReturn(false);

    // When
    boolean result = useCase.execute(secret, code);

    // Then
    assertFalse(result);
    verify(totpService).validateCode(secret, code);
  }

  @Test
  void execute_shouldDelegateToTotpService() {
    // Given
    String secret = "SECRET";
    String code = "654321";
    when(totpService.validateCode(secret, code)).thenReturn(true);

    // When
    useCase.execute(secret, code);

    // Then
    verify(totpService).validateCode(secret, code);
  }
}
