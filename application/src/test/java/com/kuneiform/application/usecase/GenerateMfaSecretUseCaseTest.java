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
class GenerateMfaSecretUseCaseTest {

  @Mock private TotpService totpService;

  private GenerateMfaSecretUseCase useCase;

  @BeforeEach
  void setUp() {
    useCase = new GenerateMfaSecretUseCase(totpService);
  }

  @Test
  void execute_shouldReturnGeneratedSecret() {
    // Given
    String expectedSecret = "JBSWY3DPEHPK3PXP";
    when(totpService.generateSecret()).thenReturn(expectedSecret);

    // When
    String result = useCase.execute();

    // Then
    assertEquals(expectedSecret, result);
    verify(totpService, times(1)).generateSecret();
  }

  @Test
  void execute_shouldDelegateToTotpService() {
    // Given
    when(totpService.generateSecret()).thenReturn("SECRET123");

    // When
    useCase.execute();

    // Then
    verify(totpService).generateSecret();
  }
}
