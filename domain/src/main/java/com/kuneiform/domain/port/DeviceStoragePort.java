package com.kuneiform.domain.port;

import com.kuneiform.domain.model.UserDevice;
import java.util.List;
import java.util.Optional;

/**
 * Port interface for device storage operations. Manages trusted devices associated with user
 * accounts.
 */
public interface DeviceStoragePort {

  /**
   * Find all devices for a specific user.
   *
   * @param userId The user's ID
   * @return List of user's devices
   */
  List<UserDevice> findByUserId(String userId);

  /**
   * Find a device by its ID.
   *
   * @param deviceId The device identifier
   * @return Optional containing the device if found
   */
  Optional<UserDevice> findByDeviceId(String deviceId);

  /**
   * Save or update a device.
   *
   * @param device The device to save
   * @return The saved device
   */
  UserDevice save(UserDevice device);

  /**
   * Delete a device (revoke device access).
   *
   * @param deviceId The device identifier
   */
  void deleteByDeviceId(String deviceId);

  /**
   * Delete all devices for a user.
   *
   * @param userId The user's ID
   */
  void deleteByUserId(String userId);

  /**
   * Check if a device exists.
   *
   * @param deviceId The device identifier
   * @return true if device exists
   */
  boolean existsByDeviceId(String deviceId);
}
