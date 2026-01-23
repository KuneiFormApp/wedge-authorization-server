package com.kuneiform.infrastructure.adapter;

import com.kuneiform.domain.model.UserDevice;
import com.kuneiform.domain.port.DeviceStoragePort;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * In-memory implementation of DeviceStoragePort. Stores devices in a ConcurrentHashMap for
 * thread-safe access. Suitable for development and single-instance deployments.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "wedge.device.storage-type", havingValue = "in-memory")
public class InMemoryDeviceStorageAdapter implements DeviceStoragePort {

  // Map of deviceId -> UserDevice
  private final Map<String, UserDevice> devices = new ConcurrentHashMap<>();

  @Override
  public List<UserDevice> findByUserId(String userId) {
    return devices.values().stream()
        .filter(device -> device.getUserId().equals(userId))
        .collect(Collectors.toList());
  }

  @Override
  public Optional<UserDevice> findByDeviceId(String deviceId) {
    return Optional.ofNullable(devices.get(deviceId));
  }

  @Override
  public UserDevice save(UserDevice device) {
    devices.put(device.getDeviceId(), device);
    log.debug("Saved device: {} for user: {}", device.getDeviceId(), device.getUserId());
    return device;
  }

  @Override
  public void deleteByDeviceId(String deviceId) {
    UserDevice removed = devices.remove(deviceId);
    if (removed != null) {
      log.info("Deleted device: {} for user: {}", deviceId, removed.getUserId());
    }
  }

  @Override
  public void deleteByUserId(String userId) {
    List<String> deviceIdsToRemove =
        devices.values().stream()
            .filter(device -> device.getUserId().equals(userId))
            .map(UserDevice::getDeviceId)
            .collect(Collectors.toList());

    deviceIdsToRemove.forEach(devices::remove);
    log.info("Deleted {} device(s) for user: {}", deviceIdsToRemove.size(), userId);
  }

  @Override
  public boolean existsByDeviceId(String deviceId) {
    return devices.containsKey(deviceId);
  }
}
