package com.kuneiform.infraestructure.security;

import com.kuneiform.infraestructure.security.HttpUserAuthenticationProvider.MfaRequiredException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

/**
 * Custom authentication failure handler that handles MfaRequiredException. Redirects to the MFA
 * flow URL if MFA is required, otherwise falls back to default behavior.
 */
@Slf4j
@Component
public class MfaAwareAuthenticationFailureHandler implements AuthenticationFailureHandler {

  private final SimpleUrlAuthenticationFailureHandler defaultHandler;

  public MfaAwareAuthenticationFailureHandler() {
    this(new SimpleUrlAuthenticationFailureHandler("/login?error=true"));
  }

  public MfaAwareAuthenticationFailureHandler(
      SimpleUrlAuthenticationFailureHandler defaultHandler) {
    this.defaultHandler = defaultHandler;
  }

  @Override
  public void onAuthenticationFailure(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
      throws IOException, ServletException {

    if (exception instanceof MfaRequiredException mfaException) {
      // Capture the original request URL so we can redirect back after MFA
      var session = request.getSession(false);
      if (session != null) {
        var savedRequest =
            (org.springframework.security.web.savedrequest.SavedRequest)
                session.getAttribute("SPRING_SECURITY_SAVED_REQUEST");
        if (savedRequest != null) {
          String originalUrl = savedRequest.getRedirectUrl();
          log.info(
              "Found saved request during MFA required, storing MFA redirect URL: {}", originalUrl);
          session.setAttribute("MFA_REDIRECT_URL", originalUrl);
        } else {
          log.warn(
              "No saved request found in session during MFA required - OAuth flow may not redirect properly");
        }
      } else {
        log.warn("No session available during MFA required");
      }

      String redirectUrl = mfaException.getRedirectUrl();
      log.info("MFA required, redirecting to: {}", redirectUrl);
      response.sendRedirect(request.getContextPath() + redirectUrl);
      return;
    }

    log.debug("Authentication failed: {}, defaulting to error page", exception.getMessage());
    defaultHandler.onAuthenticationFailure(request, response, exception);
  }
}
