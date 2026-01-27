package com.kuneiform.infrastructure.security;

import com.kuneiform.domain.model.User;
import com.kuneiform.domain.model.UserDevice;
import com.kuneiform.domain.port.DeviceStoragePort;
import com.kuneiform.infrastructure.service.DeviceFingerprintService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Customizes JWT tokens (access_token and id_token) by mapping User domain model fields into JWT
 * claims. This ensures that user information from the external user provider is properly included
 * in the issued tokens.
 *
 * <p>Also tracks devices when refresh tokens are issued for active session management.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WedgeTokenCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {

  private final DeviceStoragePort deviceStorage;
  private final DeviceFingerprintService fingerprintService;

  @Override
  public void customize(JwtEncodingContext context) {
    Authentication principal = context.getPrincipal();

    log.info(
        "WedgeTokenCustomizer invoked - token type: {}, principal class: {}",
        context.getTokenType().getValue(),
        principal.getPrincipal().getClass().getName());

    // Extract the User object from the authentication principal
    if (!(principal.getPrincipal() instanceof User)) {
      log.warn(
          "Principal is not a User object, skipping token customization. Principal type: {}",
          principal.getPrincipal().getClass().getName());
      return;
    }

    User user = (User) principal.getPrincipal();

    log.debug(
        "Customizing token for user: {} (userId: {}), token type: {}",
        user.getUsername(),
        user.getUserId(),
        context.getTokenType().getValue());

    // Customize claims for both access_token and id_token
    context
        .getClaims()
        .claims(
            claims -> {
              // Override 'sub' claim with userId instead of username
              claims.put("sub", user.getUserId());

              // Add email claim
              if (user.getEmail() != null) {
                claims.put("email", user.getEmail());
              }

              // Add username as a separate claim (since sub is now userId)
              claims.put("username", user.getUsername());

              // Map all metadata fields directly into the token payload
              if (user.getMetadata() != null && !user.getMetadata().isEmpty()) {
                for (Map.Entry<String, Object> entry : user.getMetadata().entrySet()) {
                  String key = entry.getKey();
                  Object value = entry.getValue();

                  // Add metadata to token claims
                  claims.put(key, value);

                  log.trace("Added metadata claim: {} = {}", key, value);
                }
              }
            });

    // Additional customization specific to ID tokens
    if (OidcParameterNames.ID_TOKEN.equals(context.getTokenType().getValue())) {
      log.debug("Applied ID token customizations for user: {}", user.getUsername());
    }

    // Track device when access token is issued (refresh tokens are opaque, not JWT)
    if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
      trackDevice(context, user);
    }
  }

  /**
   * Track the device when an access token is issued. Creates or updates device record with current
   * request information.
   */
  private void trackDevice(JwtEncodingContext context, User user) {
    try {
      ServletRequestAttributes attributes =
          (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

      if (attributes == null) {
        log.debug("No HTTP request context available for device tracking");
        return;
      }

      HttpServletRequest request = attributes.getRequest();
      String userId = user.getUserId();
      String userAgent = request.getHeader("User-Agent");

      if (userAgent == null || userAgent.isBlank()) {
        log.debug("No User-Agent header found, skipping device tracking");
        return;
      }

      // Generate device fingerprint
      String deviceId = fingerprintService.generateDeviceId(userId, userAgent);
      String deviceName = fingerprintService.parseDeviceName(userAgent);
      String ipAddress = fingerprintService.extractIpAddress(request);

      String authorizationId =
          context.getAuthorization() != null ? context.getAuthorization().getId() : null;

      Instant now = Instant.now();

      UserDevice device =
          UserDevice.builder()
              .deviceId(deviceId)
              .userId(userId)
              .deviceName(deviceName)
              .userAgent(userAgent)
              .ipAddress(ipAddress)
              .firstSeen(now)
              .lastUsed(now)
              .authorizationId(authorizationId)
              .build();

      deviceStorage.save(device);
      log.info("Tracked device for user '{}': {} ({})", userId, deviceName, deviceId);

    } catch (Exception e) {
      log.error("Failed to track device", e);
    }
  }
}
