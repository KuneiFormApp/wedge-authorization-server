package com.kuneiform.infraestructure.security;

import com.kuneiform.application.usecase.AuthenticateUserUseCase;
import com.kuneiform.domain.model.User;
import com.kuneiform.infraestructure.config.properties.WedgeConfigProperties;
import jakarta.servlet.http.HttpServletRequest;
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
   *   <li>Request parameter "client_id"
   *   <li>Session attribute "SPRING_SECURITY_SAVED_REQUEST" (contains original authorization
   *       request)
   *   <li>Falls back to the first client with user-provider enabled (with warning)
   * </ol>
   *
   * @return the determined client_id, or null if no valid client is found
   */
  private String determineClientId() {
    try {
      ServletRequestAttributes attributes =
          (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

      if (attributes != null) {
        HttpServletRequest request = attributes.getRequest();

        String clientId = "";

        // Try to extract from saved request in session (after redirect from /oauth2/authorize)
        Object savedRequest =
            request.getSession(false) != null
                ? request.getSession().getAttribute("SPRING_SECURITY_SAVED_REQUEST")
                : null;

        if (savedRequest != null) {
          // The saved request typically contains the original URL with client_id
          String savedRequestStr = savedRequest.toString();
          log.trace("Checking saved request: {}", savedRequestStr);

          // Extract client_id from saved request URL if present
          if (savedRequestStr.contains("client_id=")) {
            int startIdx = savedRequestStr.indexOf("client_id=") + 10;
            int endIdx = savedRequestStr.indexOf("&", startIdx);
            if (endIdx == -1) {
              endIdx = savedRequestStr.indexOf(" ", startIdx);
            }
            if (endIdx == -1) {
              endIdx = savedRequestStr.length();
            }
            clientId = savedRequestStr.substring(startIdx, endIdx);
            log.debug("Extracted client_id from saved request: {}", clientId);
            if (isClientValid(clientId)) {
              return clientId;
            }
          }
        }
      }
    } catch (Exception e) {
      log.warn("Error extracting client_id from request context: {}", e.getMessage());
    }

    // Fallback: Use the first client with user-provider enabled
    log.warn(
        "Could not determine client_id from request context, falling back to first available client");
    return config.getClients().stream()
        .filter(client -> client.getUserProvider() != null && client.getUserProvider().isEnabled())
        .map(WedgeConfigProperties.ClientConfig::getClientId)
        .findFirst()
        .orElse(null);
  }

  /**
   * Validates that the given client_id exists and has a user provider configured.
   *
   * @param clientId the client ID to validate
   * @return true if the client exists and has a user provider enabled
   */
  private boolean isClientValid(String clientId) {
    return config.getClients().stream()
        .anyMatch(
            client ->
                client.getClientId().equals(clientId)
                    && client.getUserProvider() != null
                    && client.getUserProvider().isEnabled());
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
}
