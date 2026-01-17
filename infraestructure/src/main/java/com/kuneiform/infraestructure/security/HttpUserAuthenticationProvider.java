package com.kuneiform.infraestructure.security;

import com.kuneiform.application.usecase.AuthenticateUserUseCase;
import com.kuneiform.domain.model.User;
import com.kuneiform.infraestructure.config.properties.WedgeConfigProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.WebAttributes;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Custom authentication provider that uses the HTTP-based user provider to authenticate users.
 *
 * <p>Handles MFA flow by checking user's MFA status after successful password authentication: - If
 * MFA is enabled but not registered: redirect to MFA setup - If MFA is enabled and registered:
 * redirect to MFA verification - If MFA is not enabled: complete authentication immediately
 *
 * <p>MFA session attributes: - MFA_PENDING_USER: User waiting for MFA verification - MFA_VERIFIED:
 * Flag indicating MFA verification completed - CLIENT_ID: Client ID for the current authentication
 * request
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HttpUserAuthenticationProvider implements AuthenticationProvider {

  // Session attribute keys for MFA flow
  public static final String MFA_PENDING_USER_ATTR = "MFA_PENDING_USER";
  public static final String MFA_VERIFIED_ATTR = "MFA_VERIFIED";
  public static final String CLIENT_ID_ATTR = "CLIENT_ID";

  private final AuthenticateUserUseCase authenticateUserUseCase;
  private final WedgeConfigProperties config;

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    String username = authentication.getName();
    String password = authentication.getCredentials().toString();

    log.debug("Authenticating user via HTTP provider: {}", username);

    // Extract client/tenant context from the current request
    ClientContext clientContext = determineClientContext();

    if (clientContext.clientId() == null && clientContext.tenantId() == null) {
      log.error("No client or tenant context found for authentication");
      throw new BadCredentialsException("Authentication service unavailable");
    }

    log.debug(
        "Using context client='{}' tenant='{}' for user authentication",
        clientContext.clientId(),
        clientContext.tenantId());

    Optional<User> userOpt =
        authenticateUserUseCase.execute(
            clientContext.clientId(), clientContext.tenantId(), username, password);

    if (userOpt.isEmpty()) {
      log.warn("Authentication failed for user: {}", username);
      throw new BadCredentialsException("Invalid username or password");
    }

    User user = userOpt.get();
    String effectiveClientId =
        clientContext.clientId() != null ? clientContext.clientId() : "unknown-client";

    log.info(
        "User authenticated successfully: {} (client: {}, tenant: {})",
        username,
        clientContext.clientId(),
        clientContext.tenantId());

    // Check if MFA is required for this user
    if (Boolean.TRUE.equals(user.getMfaEnabled())) {
      log.debug("MFA is enabled for user: {}", username);
      return handleMfaFlow(user, effectiveClientId, password);
    }

    // No MFA required - complete authentication
    log.debug("No MFA required for user: {}", username);
    Collection<? extends GrantedAuthority> authorities = extractAuthorities(user);
    return new UsernamePasswordAuthenticationToken(user, password, authorities);
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
  }

  private record ClientContext(String clientId, String tenantId) {}

  /** Determines the client/tenant context for the current authentication request. */
  private ClientContext determineClientContext() {
    try {
      HttpServletRequest request = getCurrentRequest();

      // 1. From request parameter
      String clientId = fromRequestParameter(request);
      if (clientId != null) {
        return new ClientContext(clientId, null);
      }

      // 2. From saved request (OAuth redirect)
      clientId = fromSavedRequest(request);
      if (clientId != null) {
        return new ClientContext(clientId, null);
      }

      // 3. Fallback to default client for direct login without OAuth context
      String defaultClientId = config.getLogin().getDefaultClientId();

      if (defaultClientId != null && !defaultClientId.isBlank()) {
        log.info("No client_id found, using defaultClientId: {}", defaultClientId);
        return new ClientContext(defaultClientId, null);
      }

      // Final fallback - use first tenant (legacy behavior)
      String defaultTenantId =
          config.getTenants().isEmpty() ? "default-tenant" : config.getTenants().get(0).getId();

      log.warn("No defaultClientId configured, falling back to tenant ID: {}", defaultTenantId);
      // Here we explicitly return it as tenantId, and clientId as null
      return new ClientContext(null, defaultTenantId);

    } catch (Exception e) {
      log.error("Error determining client context", e);

      // Emergency fallback to default client
      String defaultClientId = config.getLogin().getDefaultClientId();
      if (defaultClientId != null && !defaultClientId.isBlank()) {
        return new ClientContext(defaultClientId, null);
      }

      // Final fallback to default tenant
      String defaultTenantId =
          config.getTenants().isEmpty() ? "default-tenant" : config.getTenants().get(0).getId();

      return new ClientContext(null, defaultTenantId);
    }
  }

  private Collection<? extends GrantedAuthority> extractAuthorities(User user) {
    List<GrantedAuthority> authorities = new ArrayList<>();

    if (user.getMetadata() != null && user.getMetadata().containsKey("authorities")) {
      Object authoritiesObj = user.getMetadata().get("authorities");

      if (authoritiesObj instanceof Iterable<?>) {
        for (Object authority : (Iterable<?>) authoritiesObj) {
          String authorityStr = authority.toString();
          authorities.add(new SimpleGrantedAuthority(authorityStr));
        }
      }
    }

    if (authorities.isEmpty()) {
      authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
    }

    return authorities;
  }

  private HttpServletRequest getCurrentRequest() {
    ServletRequestAttributes attributes =
        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

    if (attributes == null) {
      throw new BadCredentialsException("No request context available");
    }

    return attributes.getRequest();
  }

  private String fromRequestParameter(HttpServletRequest request) {
    String clientId = request.getParameter("client_id");

    if (StringUtils.hasText(clientId)) {
      log.debug("Found client_id in request parameter: {}", clientId);
      return clientId;
    }

    return null;
  }

  private String fromSavedRequest(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    if (session == null) {
      return null;
    }

    Object saved = session.getAttribute("SPRING_SECURITY_SAVED_REQUEST");
    if (saved == null) {
      return null;
    }

    // Preferred: Spring Security SavedRequest
    if (saved instanceof org.springframework.security.web.savedrequest.SavedRequest sr) {
      String[] values = sr.getParameterValues("client_id");
      if (values != null && values.length > 0 && StringUtils.hasText(values[0])) {
        log.debug("Extracted client_id from SavedRequest: {}", values[0]);
        return values[0];
      }
      return null;
    }

    // Fallback (legacy / defensive)
    return extractClientIdFromString(saved.toString());
  }

  private String extractClientIdFromString(String value) {
    int idx = value.indexOf("client_id=");
    if (idx == -1) {
      return null;
    }

    int start = idx + "client_id=".length();
    int end = value.indexOf("&", start);
    if (end == -1) {
      end = value.indexOf(" ", start);
    }
    if (end == -1) {
      end = value.length();
    }

    String clientId = value.substring(start, end);
    if (StringUtils.hasText(clientId)) {
      log.debug("Extracted client_id from saved request string: {}", clientId);
      return clientId;
    }

    return null;
  }

  /**
   * Handles MFA flow: determines if user needs to set up MFA, verify their TOTP code, or complete
   * authentication (MFA already verified in this session).
   */
  private Authentication handleMfaFlow(User user, String clientId, String password) {
    HttpServletRequest request = getCurrentRequest();
    HttpSession session = request.getSession(true);

    // Check if MFA has already been verified in this session
    Boolean mfaVerified = (Boolean) session.getAttribute(MFA_VERIFIED_ATTR);
    if (Boolean.TRUE.equals(mfaVerified)) {
      log.info("MFA already verified for user: {}", user.getUsername());

      // Clear MFA verification flag
      session.removeAttribute(MFA_VERIFIED_ATTR);
      session.removeAttribute(MFA_PENDING_USER_ATTR);

      // Complete authentication
      Collection<? extends GrantedAuthority> authorities = extractAuthorities(user);
      return new UsernamePasswordAuthenticationToken(user, password, authorities);
    }

    // Store user and client ID in session for MFA flow
    session.setAttribute(MFA_PENDING_USER_ATTR, user);
    session.setAttribute(CLIENT_ID_ATTR, clientId);

    // Determine if user needs MFA setup or verification
    boolean mfaRegistered = user.getMfaData() != null && user.getMfaData().isTwoFaRegistered();

    if (mfaRegistered) {
      log.info("User needs MFA verification: {}", user.getUsername());
      // Store an attribute to trigger redirect to /mfa/verify
      session.setAttribute(
          WebAttributes.AUTHENTICATION_EXCEPTION,
          new MfaRequiredException("MFA verification required", "/mfa/verify"));
    } else {
      log.info("User needs MFA setup: {}", user.getUsername());
      // Store an attribute to trigger redirect to /mfa/setup
      session.setAttribute(
          WebAttributes.AUTHENTICATION_EXCEPTION,
          new MfaRequiredException("MFA setup required", "/mfa/setup"));
    }

    // Throw exception to prevent completing authentication
    // This will trigger the authentication failure handler
    throw new MfaRequiredException("MFA required", mfaRegistered ? "/mfa/verify" : "/mfa/setup");
  }

  public static class MfaRequiredException extends AuthenticationException {
    private final String redirectUrl;

    public MfaRequiredException(String msg, String redirectUrl) {
      super(msg);
      this.redirectUrl = redirectUrl;
    }

    public String getRedirectUrl() {
      return redirectUrl;
    }
  }
}
