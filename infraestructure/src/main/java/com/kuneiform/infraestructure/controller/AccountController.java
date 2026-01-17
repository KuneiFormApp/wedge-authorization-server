package com.kuneiform.infraestructure.controller;

import com.kuneiform.application.usecase.ListUserConsentsUseCase;
import com.kuneiform.application.usecase.ListUserDevicesUseCase;
import com.kuneiform.application.usecase.RevokeConsentUseCase;
import com.kuneiform.application.usecase.RevokeDeviceUseCase;
import com.kuneiform.domain.model.User;
import com.kuneiform.domain.model.UserConsent;
import com.kuneiform.domain.model.UserDevice;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for user account management page. Allows users to view and revoke their authorized
 * applications and active sessions.
 */
@Slf4j
@Controller
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {

  private final ListUserConsentsUseCase listUserConsentsUseCase;
  private final RevokeConsentUseCase revokeConsentUseCase;
  private final ListUserDevicesUseCase listUserDevicesUseCase;
  private final RevokeDeviceUseCase revokeDeviceUseCase;

  /** Display the account management page. Shows authorized applications and active sessions. */
  @GetMapping
  public String accountPage(Authentication authentication, Model model) {
    User user = (User) authentication.getPrincipal();

    log.debug("Loading account page for user: {}", user.getUserId());

    // Fetch consents and devices
    List<UserConsent> consents = listUserConsentsUseCase.execute(user.getUserId());
    List<UserDevice> devices = listUserDevicesUseCase.execute(user.getUserId());

    // Add to model
    model.addAttribute("user", user);
    model.addAttribute("consents", consents);
    model.addAttribute("devices", devices);

    log.info(
        "Account page loaded for user: {} ({} consents, {} devices)",
        user.getUserId(),
        consents.size(),
        devices.size());

    return "account";
  }

  /** Revoke authorization consent for a specific client. */
  @PostMapping("/consents/{clientId}/revoke")
  public String revokeConsent(
      @PathVariable String clientId,
      Authentication authentication,
      RedirectAttributes redirectAttributes) {

    User user = (User) authentication.getPrincipal();

    log.info("User {} revoking consent for client: {}", user.getUserId(), clientId);

    try {
      revokeConsentUseCase.execute(user.getUserId(), clientId);
      redirectAttributes.addFlashAttribute("success", "account.consent.revoked");
      log.info(
          "Successfully revoked consent for user: {} and client: {}", user.getUserId(), clientId);
    } catch (IllegalArgumentException e) {
      log.warn("Failed to revoke consent: {}", e.getMessage());
      redirectAttributes.addFlashAttribute("error", "account.consent.notFound");
    } catch (Exception e) {
      log.error(
          "Error revoking consent for user: {} and client: {}", user.getUserId(), clientId, e);
      redirectAttributes.addFlashAttribute("error", "account.consent.error");
    }

    return "redirect:/account";
  }

  /** Revoke a specific device. */
  @PostMapping("/devices/{deviceId}/revoke")
  public String revokeDevice(
      @PathVariable String deviceId,
      Authentication authentication,
      RedirectAttributes redirectAttributes) {

    User user = (User) authentication.getPrincipal();

    log.info("User {} revoking device: {}", user.getUserId(), deviceId);

    try {
      boolean revoked = revokeDeviceUseCase.execute(user.getUserId(), deviceId);

      if (revoked) {
        redirectAttributes.addFlashAttribute("success", "account.device.revoked");
        log.info("Successfully revoked device: {} for user: {}", deviceId, user.getUserId());
      } else {
        redirectAttributes.addFlashAttribute("error", "account.device.notFound");
        log.warn("Device not found: {} for user: {}", deviceId, user.getUserId());
      }
    } catch (Exception e) {
      log.error("Error revoking device: {} for user: {}", deviceId, user.getUserId(), e);
      redirectAttributes.addFlashAttribute("error", "account.device.error");
    }

    return "redirect:/account";
  }
}
