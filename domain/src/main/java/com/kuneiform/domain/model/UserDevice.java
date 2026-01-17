package com.kuneiform.domain.model;

import java.time.Instant;
import lombok.Builder;
import lombok.Value;

/**
 * Domain model representing a user's trusted device/browser. Tracks devices that have active
 * refresh tokens.
 */
@Value
@Builder
public class UserDevice {
  /** Unique device identifier (hash of userId + userAgent + firstSeen) */
  String deviceId;

  /** User ID who owns this device */
  String userId;

  /** Human-readable device name (e.g., "Chrome 120 on Windows") */
  String deviceName;

  /** Full user agent string */
  String userAgent;

  /** Last known IP address */
  String ipAddress;

  /** When this device was first seen */
  Instant firstSeen;

  /** When this device was last used (last token refresh) */
  Instant lastUsed;

  /** Associated authorization ID (links to oauth2_authorization table) */
  String authorizationId;
}
