package com.kuneiform.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.kuneiform.domain.model.UserDevice;
import com.kuneiform.domain.port.DeviceStoragePort;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListUserDevicesUseCaseTest {

  @Mock private DeviceStoragePort deviceStorage;

  private ListUserDevicesUseCase useCase;

  @BeforeEach
  void setUp() {
    useCase = new ListUserDevicesUseCase(deviceStorage);
  }

  @Test
  void shouldReturnDevicesWhenUserHasTrustedDevices() {
    // Given
    String userId = "user-123";

    UserDevice device1 =
        UserDevice.builder()
            .deviceId("device-1")
            .userId(userId)
            .deviceName("Chrome on Windows")
            .userAgent("Mozilla/5.0...")
            .firstSeen(Instant.now())
            .lastUsed(Instant.now())
            .build();

    UserDevice device2 =
        UserDevice.builder()
            .deviceId("device-2")
            .userId(userId)
            .deviceName("Safari on iPhone")
            .userAgent("Mozilla/5.0...")
            .firstSeen(Instant.now())
            .lastUsed(Instant.now())
            .build();

    when(deviceStorage.findByUserId(userId)).thenReturn(List.of(device1, device2));

    // When
    List<UserDevice> result = useCase.execute(userId);

    // Then
    assertNotNull(result);
    assertEquals(2, result.size());
    assertTrue(result.contains(device1));
    assertTrue(result.contains(device2));
    verify(deviceStorage).findByUserId(userId);
  }

  @Test
  void shouldReturnEmptyListWhenUserHasNoDevices() {
    // Given
    String userId = "user-no-devices";

    when(deviceStorage.findByUserId(userId)).thenReturn(Collections.emptyList());

    // When
    List<UserDevice> result = useCase.execute(userId);

    // Then
    assertNotNull(result);
    assertTrue(result.isEmpty());
    verify(deviceStorage).findByUserId(userId);
  }
}
