package com.kuneiform.infrastructure.service;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ua_parser.Client;
import ua_parser.Parser;

/**
 * Service for generating device fingerprints and parsing device information. Uses user-agent
 * parsing to identify browser and OS.
 */
@Slf4j
@Service
public class DeviceFingerprintService {

  private final Parser uaParser;

  public DeviceFingerprintService() {
    this.uaParser = new Parser();
  }

  /**
   * Generate a unique device ID from user information. Format: hash(userId + userAgent + date) Same
   * device on same day = same ID
   *
   * @param userId User identifier
   * @param userAgent User agent string
   * @return Device fingerprint (hex string)
   */
  public String generateDeviceId(String userId, String userAgent) {
    try {
      // Normalize to current date to group same device/browser
      String dateKey = LocalDate.now().toString();
      String input = userId + "|" + normalizeUserAgent(userAgent) + "|" + dateKey;

      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

      // Take first 16 bytes for shorter ID
      byte[] shortHash = new byte[16];
      System.arraycopy(hash, 0, shortHash, 0, 16);

      return HexFormat.of().formatHex(shortHash);
    } catch (NoSuchAlgorithmException e) {
      log.error("SHA-256 algorithm not available", e);
      // Fallback to simple hash
      return String.valueOf((userId + userAgent).hashCode());
    }
  }

  /**
   * Parse user agent to get human-readable device name.
   *
   * @param userAgent User agent string
   * @return Device name (e.g., "Chrome 120 on Windows")
   */
  public String parseDeviceName(String userAgent) {
    if (userAgent == null || userAgent.isBlank()) {
      return "Unknown Device";
    }

    try {
      Client client = uaParser.parse(userAgent);

      String browser = client.userAgent.family;
      String browserVersion = client.userAgent.major;
      String os = client.os.family;

      StringBuilder name = new StringBuilder();

      // Browser name and version
      if (browser != null && !browser.equals("Other")) {
        name.append(browser);
        if (browserVersion != null) {
          name.append(" ").append(browserVersion);
        }
      } else {
        name.append("Unknown Browser");
      }

      // Operating system
      if (os != null && !os.equals("Other")) {
        name.append(" on ").append(os);
      }

      return name.toString();
    } catch (Exception e) {
      log.warn("Failed to parse user agent: {}", userAgent, e);
      return "Unknown Device";
    }
  }

  /**
   * Extract IP address from HTTP request.
   *
   * @param request HTTP request
   * @return IP address
   */
  public String extractIpAddress(HttpServletRequest request) {
    // Check for proxy headers first
    String ip = request.getHeader("X-Forwarded-For");
    if (ip != null && !ip.isBlank()) {
      // Take first IP if multiple
      return ip.split(",")[0].trim();
    }

    ip = request.getHeader("X-Real-IP");
    if (ip != null && !ip.isBlank()) {
      return ip;
    }

    // Fallback to remote address
    return request.getRemoteAddr();
  }

  /** Normalize user agent to handle minor version differences. */
  private String normalizeUserAgent(String userAgent) {
    if (userAgent == null) {
      return "";
    }

    // Remove version micro-numbers and build info to group similar browsers
    return userAgent
        .replaceAll("\\d+\\.\\d+\\.\\d+\\.\\d+", "x.x.x.x") // Version numbers
        .replaceAll("Build/[A-Z0-9]+", "Build/XXX"); // Build identifiers
  }
}
