package com.kuneiform.infrastructure.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.kuneiform.domain.model.UserDevice;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InMemoryDeviceStorageAdapterTest {

  private InMemoryDeviceStorageAdapter adapter;

  @BeforeEach
  void setUp() {
    adapter = new InMemoryDeviceStorageAdapter();
  }

  @Test
  void shouldSaveAndRetrieveDevice() {
    String deviceId = "device-123";
    UserDevice device =
        UserDevice.builder()
            .deviceId(deviceId)
            .userId("user-1")
            .deviceName("Test Device")
            .firstSeen(Instant.now())
            .build();

    adapter.save(device);

    Optional<UserDevice> retrieved = adapter.findByDeviceId(deviceId);
    assertThat(retrieved).isPresent();
    assertThat(retrieved.get().getDeviceName()).isEqualTo("Test Device");
  }

  @Test
  void shouldFindDevicesByUserId() {
    String userId = "user-A";

    UserDevice device1 = UserDevice.builder().deviceId("d1").userId(userId).build();

    UserDevice device2 = UserDevice.builder().deviceId("d2").userId(userId).build();

    UserDevice deviceOther = UserDevice.builder().deviceId("d3").userId("user-B").build();

    adapter.save(device1);
    adapter.save(device2);
    adapter.save(deviceOther);

    List<UserDevice> devices = adapter.findByUserId(userId);
    assertThat(devices)
        .hasSize(2)
        .extracting(UserDevice::getDeviceId)
        .containsExactlyInAnyOrder("d1", "d2");
  }

  @Test
  void shouldDeleteDeviceByDeviceId() {
    String deviceId = "to-delete";
    UserDevice device = UserDevice.builder().deviceId(deviceId).userId("user-X").build();

    adapter.save(device);
    assertThat(adapter.existsByDeviceId(deviceId)).isTrue();

    adapter.deleteByDeviceId(deviceId);
    assertThat(adapter.existsByDeviceId(deviceId)).isFalse();
    assertThat(adapter.findByDeviceId(deviceId)).isEmpty();
  }

  @Test
  void shouldDeleteDevicesByUserId() {
    String userId = "remove-all";

    adapter.save(UserDevice.builder().deviceId("d10").userId(userId).build());
    adapter.save(UserDevice.builder().deviceId("d11").userId(userId).build());
    adapter.save(UserDevice.builder().deviceId("d12").userId("keep").build());

    adapter.deleteByUserId(userId);

    assertThat(adapter.findByUserId(userId)).isEmpty();
    assertThat(adapter.existsByDeviceId("d12")).isTrue();
  }
}
