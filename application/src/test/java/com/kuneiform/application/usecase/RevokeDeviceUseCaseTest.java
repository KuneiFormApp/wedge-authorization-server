package com.kuneiform.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.kuneiform.domain.model.UserDevice;
import com.kuneiform.domain.port.AuthorizationRevocationPort;
import com.kuneiform.domain.port.DeviceStoragePort;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RevokeDeviceUseCaseTest {

  @Mock private DeviceStoragePort deviceStorage;
  @Mock private AuthorizationRevocationPort authorizationRevocation;

  private RevokeDeviceUseCase useCase;

  @BeforeEach
  void setUp() {
    useCase = new RevokeDeviceUseCase(deviceStorage, authorizationRevocation);
  }

  @Test
  void shouldRevokeDeviceAndAuthorizationWhenDeviceBelongsToUser() {
    // Given
    String userId = "user-123";
    String deviceId = "device-abc";
    String authId = "auth-xyz";

    UserDevice device =
        UserDevice.builder()
            .deviceId(deviceId)
            .userId(userId)
            .authorizationId(authId)
            .deviceName("Test Device")
            .userAgent("Mozilla/5.0")
            .firstSeen(Instant.now())
            .lastUsed(Instant.now())
            .build();

    when(deviceStorage.findByDeviceId(deviceId)).thenReturn(Optional.of(device));

    // When
    boolean result = useCase.execute(userId, deviceId);

    // Then
    assertTrue(result);
    // Verify auth revoked first
    verify(authorizationRevocation).revokeById(authId);
    // Then device deleted
    verify(deviceStorage).deleteByDeviceId(deviceId);
  }

  @Test
  void shouldStillRevokeDeviceWhenNoAuthorizationIdPresent() {
    // Given
    String userId = "user-123";
    String deviceId = "device-abc";

    // Device with no active auth
    UserDevice device =
        UserDevice.builder()
            .deviceId(deviceId)
            .userId(userId)
            .authorizationId(null) // No auth ID
            .deviceName("Test Device")
            .build();

    when(deviceStorage.findByDeviceId(deviceId)).thenReturn(Optional.of(device));

    // When
    boolean result = useCase.execute(userId, deviceId);

    // Then
    assertTrue(result);
    verifyNoInteractions(authorizationRevocation); // Should skip auth revocation
    verify(deviceStorage).deleteByDeviceId(deviceId); // But still delete device
  }

  @Test
  void shouldReturnFalseWhenDeviceNotFound() {
    // Given
    String userId = "user-123";
    String deviceId = "unknown-device";

    when(deviceStorage.findByDeviceId(deviceId)).thenReturn(Optional.empty());

    // When
    boolean result = useCase.execute(userId, deviceId);

    // Then
    assertFalse(result);
    verify(deviceStorage, never()).deleteByDeviceId(any());
  }

  @Test
  void shouldReturnFalseWhenDeviceBelongsToAnotherUser() {
    // Given
    String userId = "user-123";
    String deviceId = "device-abc";
    String actualOwnerId = "user-789";

    UserDevice device =
        UserDevice.builder()
            .deviceId(deviceId)
            .userId(actualOwnerId) // Different owner
            .build();

    when(deviceStorage.findByDeviceId(deviceId)).thenReturn(Optional.of(device));

    // When
    boolean result = useCase.execute(userId, deviceId);

    // Then
    assertFalse(result);
    verify(deviceStorage, never()).deleteByDeviceId(any());
  }
}
