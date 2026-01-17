package com.kuneiform.application.usecase;

import com.kuneiform.domain.model.UserDevice;
import com.kuneiform.domain.port.DeviceStoragePort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Use case for listing all trusted devices for a user. Returns devices with active refresh tokens.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ListUserDevicesUseCase {

  private final DeviceStoragePort deviceStorage;

  /**
   * Execute the use case to list all devices for a user.
   *
   * @param userId The user's ID
   * @return List of user's trusted devices
   */
  public List<UserDevice> execute(String userId) {
    log.debug("Listing devices for user: {}", userId);

    List<UserDevice> devices = deviceStorage.findByUserId(userId);

    log.info("Found {} device(s) for user: {}", devices.size(), userId);
    return devices;
  }
}
