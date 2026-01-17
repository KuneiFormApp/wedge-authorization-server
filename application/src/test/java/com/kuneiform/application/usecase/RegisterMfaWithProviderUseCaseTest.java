package com.kuneiform.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.kuneiform.domain.port.MfaRegistrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegisterMfaWithProviderUseCaseTest {

  @Mock private MfaRegistrationService mfaRegistrationService;

  private RegisterMfaWithProviderUseCase useCase;

  @BeforeEach
  void setUp() {
    useCase = new RegisterMfaWithProviderUseCase(mfaRegistrationService);
  }

  @Test
  void execute_shouldReturnTrueWhenRegistrationSucceeds() {
    // Given
    String clientId = "test-client";
    String userId = "user123";
    String mfaSecret = "JBSWY3DPEHPK3PXP";
    String mfaKeyId = "WedgeAuth:user@example.com";

    when(mfaRegistrationService.registerMfa(clientId, userId, mfaSecret, mfaKeyId))
        .thenReturn(true);

    // When
    boolean result = useCase.execute(clientId, userId, mfaSecret, mfaKeyId);

    // Then
    assertTrue(result);
    verify(mfaRegistrationService).registerMfa(clientId, userId, mfaSecret, mfaKeyId);
  }

  @Test
  void execute_shouldReturnFalseWhenRegistrationFails() {
    // Given
    String clientId = "test-client";
    String userId = "user123";
    String mfaSecret = "SECRET";
    String mfaKeyId = "Test:user";

    when(mfaRegistrationService.registerMfa(clientId, userId, mfaSecret, mfaKeyId))
        .thenReturn(false);

    // When
    boolean result = useCase.execute(clientId, userId, mfaSecret, mfaKeyId);

    // Then
    assertFalse(result);
    verify(mfaRegistrationService).registerMfa(clientId, userId, mfaSecret, mfaKeyId);
  }

  @Test
  void execute_shouldDelegateToMfaRegistrationService() {
    // Given
    String clientId = "client-1";
    String userId = "user-1";
    String secret = "SECRET123";
    String keyId = "App:user1";

    when(mfaRegistrationService.registerMfa(clientId, userId, secret, keyId)).thenReturn(true);

    // When
    useCase.execute(clientId, userId, secret, keyId);

    // Then
    verify(mfaRegistrationService, times(1)).registerMfa(clientId, userId, secret, keyId);
  }
}
