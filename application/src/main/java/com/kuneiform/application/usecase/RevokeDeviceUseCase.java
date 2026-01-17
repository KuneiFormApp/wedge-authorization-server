package com.kuneiform.application.usecase;

import com.kuneiform.domain.port.AuthorizationRevocationPort;
import com.kuneiform.domain.port.DeviceStoragePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Use case for revoking a device (removing trusted device access). Deletes the device record and
 * invalidates associated OAuth2 authorization.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RevokeDeviceUseCase {

  private final DeviceStoragePort deviceStorage;
  private final AuthorizationRevocationPort authorizationRevocation;

  /**
   * Execute the use case to revoke a device.
   *
   * @param userId The user's ID (for authorization check)
   * @param deviceId The device identifier to revoke
   * @return true if device was revoked, false if not found
   */
  public boolean execute(String userId, String deviceId) {
    log.info("Revoking device '{}' for user: {}", deviceId, userId);

    // Verify device belongs to user
    var deviceOpt = deviceStorage.findByDeviceId(deviceId);

    if (deviceOpt.isEmpty()) {
      log.warn("Device '{}' not found", deviceId);
      return false;
    }

    var device = deviceOpt.get();
    if (!device.getUserId().equals(userId)) {
      log.warn("Device '{}' does not belong to user '{}'", deviceId, userId);
      return false;
    }

    // Revoke OAuth2 authorization associated with this device
    String authorizationId = device.getAuthorizationId();
    if (authorizationId != null && !authorizationId.isBlank()) {
      authorizationRevocation.revokeById(authorizationId);
      log.info("Revoked OAuth2 authorization '{}' for device '{}'", authorizationId, deviceId);
    }

    // Revoke device record
    deviceStorage.deleteByDeviceId(deviceId);
    log.info("Successfully revoked device '{}' for user: {}", deviceId, userId);

    return true;
  }
}
