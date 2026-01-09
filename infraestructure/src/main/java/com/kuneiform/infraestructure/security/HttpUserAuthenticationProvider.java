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
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

// Custom authentication provider that uses the HTTP-based user provider to authenticate users
@Slf4j
@Component
@RequiredArgsConstructor
public class HttpUserAuthenticationProvider implements AuthenticationProvider {

  private final AuthenticateUserUseCase authenticateUserUseCase;
  private final WedgeConfigProperties config;

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    String username = authentication.getName();
    String password = authentication.getCredentials().toString();

    log.debug("Authenticating user via HTTP provider: {}", username);

    // Extract clientId from the current request context
    String clientId = determineClientId();

    if (clientId == null) {
      log.error("No client with user-provider enabled found");
      throw new BadCredentialsException("Authentication service unavailable");
    }

    log.debug("Using client '{}' for user authentication", clientId);

    Optional<User> userOpt = authenticateUserUseCase.execute(clientId, username, password);

    if (userOpt.isEmpty()) {
      log.warn("Authentication failed for user: {}", username);
      throw new BadCredentialsException("Invalid username or password");
    }

    User user = userOpt.get();

    log.info("User authenticated successfully: {} (client: {})", username, clientId);

    Collection<? extends GrantedAuthority> authorities = extractAuthorities(user);

    // Store the User object as the principal so it's available for token
    // customization
    return new UsernamePasswordAuthenticationToken(user, password, authorities);
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
  }

  /**
   * Determines the client_id for the current authentication request.
   *
   * <p>Attempts to extract the client_id from the current HTTP request context in the following
   * order:
   *
   * <ol>
   *   <li>Request parameter "client_id" (direct from current request)
   *   <li>Session attribute "SPRING_SECURITY_SAVED_REQUEST" (contains original authorization
   *       request)
   * </ol>
   *
   * @return the determined client_id
   * @throws BadCredentialsException if client_id cannot be determined from request context
   */
  private String determineClientId() {
    try {
      HttpServletRequest request = getCurrentRequest();

      // 1. From request parameter
      String clientId = fromRequestParameter(request);
      if (clientId != null) {
        return clientId;
      }

      // 2. From saved request (OAuth redirect)
      clientId = fromSavedRequest(request);
      if (clientId != null) {
        return clientId;
      }

      throw new BadCredentialsException("client_id is required but was not found in the request");

    } catch (BadCredentialsException e) {
      throw e;
    } catch (Exception e) {
      log.error("Error determining client_id", e);
      throw new BadCredentialsException("Failed to determine client_id from request context", e);
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
}
