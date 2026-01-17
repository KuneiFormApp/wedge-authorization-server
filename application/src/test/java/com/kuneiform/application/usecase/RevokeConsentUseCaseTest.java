package com.kuneiform.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.kuneiform.domain.port.AuthorizationRevocationPort;
import com.kuneiform.domain.port.ConsentStoragePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RevokeConsentUseCaseTest {

  @Mock private ConsentStoragePort consentStorage;
  @Mock private AuthorizationRevocationPort authorizationRevocation;

  private RevokeConsentUseCase useCase;

  @BeforeEach
  void setUp() {
    useCase = new RevokeConsentUseCase(consentStorage, authorizationRevocation);
  }

  @Test
  void shouldRevokeConsentWhenConsentExists() {
    // Given
    String userId = "user-123";
    String clientId = "client-456";

    when(consentStorage.hasConsent(userId, clientId)).thenReturn(true);
    when(authorizationRevocation.revokeByUserAndClient(userId, clientId)).thenReturn(3);

    // When
    useCase.execute(userId, clientId);

    // Then - verify operations happen in correct order
    InOrder inOrder = inOrder(consentStorage, authorizationRevocation);
    inOrder.verify(consentStorage).hasConsent(userId, clientId);
    inOrder.verify(authorizationRevocation).revokeByUserAndClient(userId, clientId);
    inOrder.verify(consentStorage).revokeByClientId(userId, clientId);
  }

  @Test
  void shouldThrowExceptionWhenConsentDoesNotExist() {
    // Given
    String userId = "user-123";
    String clientId = "client-456";

    when(consentStorage.hasConsent(userId, clientId)).thenReturn(false);

    // When & Then
    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(userId, clientId));

    assertEquals("No consent found for user and client", exception.getMessage());
    verify(consentStorage).hasConsent(userId, clientId);
    verifyNoInteractions(authorizationRevocation);
    verify(consentStorage, never()).revokeByClientId(any(), any());
  }

  @Test
  void shouldRevokeTokensBeforeRemovingConsent() {
    // Given
    String userId = "user-123";
    String clientId = "client-456";

    when(consentStorage.hasConsent(userId, clientId)).thenReturn(true);
    when(authorizationRevocation.revokeByUserAndClient(userId, clientId)).thenReturn(0);

    // When
    useCase.execute(userId, clientId);

    // Then - tokens should be revoked before consent is removed
    InOrder inOrder = inOrder(authorizationRevocation, consentStorage);
    inOrder.verify(authorizationRevocation).revokeByUserAndClient(userId, clientId);
    inOrder.verify(consentStorage).revokeByClientId(userId, clientId);
  }
}
