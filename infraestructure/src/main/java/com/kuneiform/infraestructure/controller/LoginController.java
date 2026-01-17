package com.kuneiform.infraestructure.controller;

import com.kuneiform.application.usecase.GenerateMfaQrCodeUseCase;
import com.kuneiform.application.usecase.GenerateMfaSecretUseCase;
import com.kuneiform.application.usecase.RegisterMfaWithProviderUseCase;
import com.kuneiform.application.usecase.ValidateMfaTotpUseCase;
import com.kuneiform.domain.model.MfaData;
import com.kuneiform.domain.model.User;
import com.kuneiform.domain.port.QrCodeService;
import jakarta.servlet.http.HttpSession;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Controller for serving the login page and MFA flows. */
@Slf4j
@Controller
@RequiredArgsConstructor
public class LoginController {

  private static final String MFA_PENDING_USER_ATTR = "MFA_PENDING_USER";
  private static final String MFA_ENDPOINT_ATTR = "MFA_ENDPOINT";
  private static final String MFA_SECRET_ATTR = "MFA_SECRET";
  private static final String MFA_KEY_ID_ATTR = "MFA_KEY_ID";
  private static final int QR_CODE_SIZE = 300; // pixels

  private final MessageSource messageSource;
  private final GenerateMfaSecretUseCase generateMfaSecretUseCase;
  private final ValidateMfaTotpUseCase validateMfaTotpUseCase;
  private final GenerateMfaQrCodeUseCase generateMfaQrCodeUseCase;
  private final RegisterMfaWithProviderUseCase registerMfaWithProviderUseCase;
  private final com.kuneiform.infraestructure.config.properties.WedgeConfigProperties config;
  private final org.springframework.security.web.context.SecurityContextRepository
      securityContextRepository =
          new org.springframework.security.web.context.HttpSessionSecurityContextRepository();
  private final org.springframework.security.core.context.SecurityContextHolderStrategy
      securityContextHolderStrategy =
          org.springframework.security.core.context.SecurityContextHolder
              .getContextHolderStrategy();

  @GetMapping("/login")
  public String login(
      @RequestParam(required = false) String error,
      @RequestParam(required = false) String logout,
      @RequestParam(required = false) String username,
      Locale locale,
      Model model) {

    // Check if user is already authenticated
    // But ONLY redirect if this is a clean login attempt (no error, no logout)
    if (error == null && logout == null) {
      org.springframework.security.core.Authentication authentication =
          securityContextHolderStrategy.getContext().getAuthentication();
      if (authentication != null
          && authentication.isAuthenticated()
          && !(authentication
              instanceof
              org.springframework.security.authentication.AnonymousAuthenticationToken)) {

        log.info(
            "User already authenticated, redirecting from login page: {}",
            authentication.getName());

        if (authentication.getPrincipal() instanceof User user) {
          return handleDefaultRedirect(user);
        }
      }
    }

    if (error != null) {
      model.addAttribute("errorMessage", messageSource.getMessage("login.error", null, locale));
    }

    if (logout != null) {
      model.addAttribute(
          "logoutMessage", messageSource.getMessage("login.logout.success", null, locale));
    }

    if (username != null) {
      model.addAttribute("username", username);
    }

    return "login";
  }

  /**
   * MFA Setup page - displays QR code for initial MFA registration.
   *
   * <p>This page is shown when a user has MFA enabled but hasn't registered their authenticator app
   * yet.
   */
  @GetMapping("/mfa/setup")
  public String mfaSetup(HttpSession session, Model model, Locale locale) {
    // Check if user is in MFA setup state
    User user = (User) session.getAttribute(MFA_PENDING_USER_ATTR);
    if (user == null || user.getMfaData() == null) {
      log.warn("MFA setup accessed without pending user in session");
      return "redirect:/login";
    }

    MfaData mfaData = user.getMfaData();

    // If already registered, redirect to verification
    if (mfaData.isTwoFaRegistered()) {
      return "redirect:/mfa/verify";
    }

    // Generate or retrieve MFA secret from session
    String mfaSecret = (String) session.getAttribute(MFA_SECRET_ATTR);
    String mfaKeyId = (String) session.getAttribute(MFA_KEY_ID_ATTR);

    if (mfaSecret == null) {
      // Generate new secret for this setup session
      mfaSecret = generateMfaSecretUseCase.execute();
      mfaKeyId = mfaData.getMfaKeyId();

      // Store in session
      session.setAttribute(MFA_SECRET_ATTR, mfaSecret);
      session.setAttribute(MFA_KEY_ID_ATTR, mfaKeyId);
      log.info("Generated new MFA secret for user setup: {}", user.getUserId());
    }

    model.addAttribute("username", user.getUsername());
    model.addAttribute("mfaSecret", mfaSecret);
    model.addAttribute("mfaKeyId", mfaKeyId);

    return "mfa-setup";
  }

  /**
   * Regenerates the MFA secret for the current setup session. Clears the existing secret from the
   * session so a new one is generated on next access.
   */
  @PostMapping("/mfa/setup/regenerate")
  public String regenerateMfaSecret(HttpSession session) {
    User user = (User) session.getAttribute(MFA_PENDING_USER_ATTR);

    // Security check: ensure user is in valid state
    if (user == null || user.getMfaData() == null || user.getMfaData().isTwoFaRegistered()) {
      log.warn("Invalid attempt to regenerate MFA secret");
      return "redirect:/login?error";
    }

    log.info("Regenerating MFA secret for user: {}", user.getUserId());

    // Clear existing secret data to force regeneration
    session.removeAttribute(MFA_SECRET_ATTR);
    session.removeAttribute(MFA_KEY_ID_ATTR);

    return "redirect:/mfa/setup";
  }

  /** QR Code image endpoint - generates and returns QR code as PNG image. */
  @GetMapping("/mfa/qrcode")
  public ResponseEntity<byte[]> mfaQrCode(HttpSession session) {
    // Retrieve MFA data from session
    String mfaSecret = (String) session.getAttribute(MFA_SECRET_ATTR);
    String mfaKeyId = (String) session.getAttribute(MFA_KEY_ID_ATTR);

    if (mfaSecret == null || mfaKeyId == null) {
      log.warn("MFA QR code accessed without setup data in session");
      return ResponseEntity.badRequest().build();
    }

    try {
      // Extract issuer and account name from mfaKeyId (format: "Issuer:Account")
      String[] parts = mfaKeyId.split(":", 2);
      String issuer = parts.length > 0 ? parts[0] : "WedgeAuth";
      String accountName = parts.length > 1 ? parts[1] : mfaKeyId;

      // Generate QR code
      byte[] qrCodeBytes =
          generateMfaQrCodeUseCase.execute(
              issuer, accountName, mfaSecret, QR_CODE_SIZE, QR_CODE_SIZE);

      return ResponseEntity.ok()
          .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE)
          .body(qrCodeBytes);

    } catch (QrCodeService.QrCodeGenerationException e) {
      log.error("Failed to generate QR code", e);
      return ResponseEntity.internalServerError().build();
    }
  }

  /** MFA Setup Verification - verifies the initial TOTP code and completes MFA registration. */
  @PostMapping("/mfa/setup/verify")
  public String mfaSetupVerify(
      @RequestParam("code") String code, HttpSession session, Model model, Locale locale) {

    User user = (User) session.getAttribute(MFA_PENDING_USER_ATTR);
    String mfaSecret = (String) session.getAttribute(MFA_SECRET_ATTR);
    String mfaKeyId = (String) session.getAttribute(MFA_KEY_ID_ATTR);
    String clientId = (String) session.getAttribute("CLIENT_ID"); // Get clientId from session

    if (user == null || mfaSecret == null || clientId == null) {
      log.warn("MFA setup verify accessed without session data");
      return "redirect:/login?error";
    }

    // Validate the TOTP code
    boolean isValid = validateMfaTotpUseCase.execute(mfaSecret, code);

    if (!isValid) {
      log.warn("Invalid TOTP code during MFA setup for user: {}", user.getUserId());
      model.addAttribute("username", user.getUsername());
      model.addAttribute("mfaSecret", mfaSecret);
      model.addAttribute("mfaKeyId", mfaKeyId);
      model.addAttribute(
          "errorMessage", messageSource.getMessage("mfa.setup.invalid.code", null, locale));
      return "mfa-setup";
    }

    // Register MFA with user provider (adapter will resolve endpoint from clientId)
    boolean registered =
        registerMfaWithProviderUseCase.execute(clientId, user.getUserId(), mfaSecret, mfaKeyId);

    if (!registered) {
      log.error("Failed to register MFA with user provider for user: {}", user.getUserId());
      model.addAttribute("username", user.getUsername());
      model.addAttribute("mfaSecret", mfaSecret);
      model.addAttribute("mfaKeyId", mfaKeyId);
      model.addAttribute(
          "errorMessage", messageSource.getMessage("mfa.setup.registration.failed", null, locale));
      return "mfa-setup";
    }

    log.info("MFA setup completed successfully for user: {}", user.getUserId());

    // Clear MFA setup session attributes
    session.removeAttribute(MFA_SECRET_ATTR);
    session.removeAttribute(MFA_KEY_ID_ATTR);
    session.removeAttribute(MFA_PENDING_USER_ATTR);
    session.removeAttribute(MFA_ENDPOINT_ATTR);

    // Redirect to login with success message
    // Redirect to login with success message and pre-fill username
    return "redirect:/login?mfa_registered&username=" + user.getUsername();
  }

  /** MFA Verification page - for entering TOTP code during login. */
  @GetMapping("/mfa/verify")
  public String mfaVerify(HttpSession session, Model model) {
    User user = (User) session.getAttribute(MFA_PENDING_USER_ATTR);
    if (user == null) {
      log.warn("MFA verify accessed without pending user in session");
      return "redirect:/login";
    }

    model.addAttribute("username", user.getUsername());
    return "mfa-verify";
  }

  /**
   * MFA Verification - validates TOTP code during login.
   *
   * <p>Note: The actual authentication completion is handled by the authentication provider. This
   * endpoint validates the code and sets a flag in the session.
   */
  @PostMapping("/mfa/verify")
  public String mfaVerifyPost(
      @RequestParam("code") String code, HttpSession session, Model model, Locale locale) {

    User user = (User) session.getAttribute(MFA_PENDING_USER_ATTR);
    if (user == null || user.getMfaData() == null) {
      log.warn("MFA verify POST accessed without session data");
      return "redirect:/login?error";
    }

    // Validate the TOTP code
    boolean isValid = validateMfaTotpUseCase.execute(user.getMfaData().getMfaSecret(), code);

    if (!isValid) {
      log.warn("Invalid TOTP code during login for user: {}", user.getUserId());
      model.addAttribute("username", user.getUsername());
      model.addAttribute(
          "errorMessage", messageSource.getMessage("mfa.verify.invalid.code", null, locale));
      return "mfa-verify";
    }

    // Create authentication token
    org.springframework.security.core.Authentication authentication =
        new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
            user,
            null,
            java.util.Collections.singleton(
                new org.springframework.security.core.authority.SimpleGrantedAuthority(
                    "ROLE_USER")));

    // Create and set security context
    org.springframework.security.core.context.SecurityContext context =
        securityContextHolderStrategy.createEmptyContext();
    context.setAuthentication(authentication);
    securityContextHolderStrategy.setContext(context);
    securityContextRepository.saveContext(context, getCurrentRequest(), getCurrentResponse());

    log.info(
        "MFA verification successful for user: {}. Security context established.",
        user.getUserId());

    // Debug: Log all session attributes
    log.debug("Session attributes after MFA verification:");
    var attributeNames = session.getAttributeNames();
    while (attributeNames.hasMoreElements()) {
      String attrName = attributeNames.nextElement();
      log.debug("  - {}: {}", attrName, session.getAttribute(attrName));
    }

    // Clean up MFA session attributes
    session.removeAttribute(MFA_PENDING_USER_ATTR);

    // Try to get the saved request from Spring Security
    var savedRequest =
        (org.springframework.security.web.savedrequest.SavedRequest)
            session.getAttribute("SPRING_SECURITY_SAVED_REQUEST");

    if (savedRequest != null) {
      String redirectUrl = savedRequest.getRedirectUrl();
      log.info("Found saved request! Redirecting to: {}", redirectUrl);
      session.removeAttribute("SPRING_SECURITY_SAVED_REQUEST");
      return "redirect:" + redirectUrl;
    } else {
      log.warn("No SPRING_SECURITY_SAVED_REQUEST found in session");
    }

    // Fallback to MFA_REDIRECT_URL if saved request not available
    String redirectUrl = (String) session.getAttribute("MFA_REDIRECT_URL");
    if (redirectUrl != null && !redirectUrl.isBlank()) {
      log.info("Found MFA_REDIRECT_URL! Redirecting to: {}", redirectUrl);
      session.removeAttribute("MFA_REDIRECT_URL");
      return "redirect:" + redirectUrl;
    } else {
      log.warn("No MFA_REDIRECT_URL found in session");
    }

    // Default fallback - use configured redirect mode
    log.info(
        "No saved OAuth request found, using configured redirect mode: {}",
        config.getLogin().getDefaultRedirectMode());

    return handleDefaultRedirect(user);
  }

  private String handleDefaultRedirect(User user) {
    var redirectMode = config.getLogin().getDefaultRedirectMode();

    switch (redirectMode) {
      case DEFAULT_CLIENT:
        return redirectToDefaultClient();

      case ACCOUNT_PAGE:
        log.info("Redirecting to account page for user: {}", user.getUserId());
        return "redirect:/account";

      case NO_APPLICATION:
      default:
        log.info("Showing no-application page for user: {}", user.getUserId());
        return "no-application";
    }
  }

  /** Redirect to configured default client URI directly. */
  private String redirectToDefaultClient() {
    String defaultClientId = config.getLogin().getDefaultClientId();
    String defaultRedirectUri = config.getLogin().getDefaultRedirectUri();

    if (defaultClientId == null
        || defaultClientId.isBlank()
        || defaultRedirectUri == null
        || defaultRedirectUri.isBlank()) {
      log.error(
          "DEFAULT_CLIENT mode configured but defaultClientId or defaultRedirectUri is missing. "
              + "Falling back to NO_APPLICATION mode.");
      return "no-application";
    }

    log.info("Redirecting directly to default client URI: {}", defaultRedirectUri);
    return "redirect:" + defaultRedirectUri;
  }

  private jakarta.servlet.http.HttpServletRequest getCurrentRequest() {
    return ((org.springframework.web.context.request.ServletRequestAttributes)
            org.springframework.web.context.request.RequestContextHolder.getRequestAttributes())
        .getRequest();
  }

  private jakarta.servlet.http.HttpServletResponse getCurrentResponse() {
    return ((org.springframework.web.context.request.ServletRequestAttributes)
            org.springframework.web.context.request.RequestContextHolder.getRequestAttributes())
        .getResponse();
  }
}
