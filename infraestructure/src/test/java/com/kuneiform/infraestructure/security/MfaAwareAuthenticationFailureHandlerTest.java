package com.kuneiform.infraestructure.security;

import static org.mockito.Mockito.*;

import com.kuneiform.infraestructure.security.HttpUserAuthenticationProvider.MfaRequiredException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;

class MfaAwareAuthenticationFailureHandlerTest {

  private MfaAwareAuthenticationFailureHandler failureHandler;
  private HttpServletRequest request;
  private HttpServletResponse response;

  @BeforeEach
  void setUp() {
    // Create mocks
    request = mock(HttpServletRequest.class);
    response = mock(HttpServletResponse.class);

    // Default context path setup
    lenient().when(request.getContextPath()).thenReturn("");
    // Mock encodeRedirectURL to return the input (needed by
    // DefaultRedirectStrategy)
    lenient()
        .when(response.encodeRedirectURL(anyString()))
        .thenAnswer(invocation -> invocation.getArgument(0));
    // Mock getSession to return null (no session creation for failure handler)
    lenient().when(request.getSession(false)).thenReturn(null);

    // Create handler after mocks are configured
    failureHandler = new MfaAwareAuthenticationFailureHandler();
  }

  @Test
  void onAuthenticationFailure_WithMfaRequiredException_ShouldRedirectToMfaUrl() throws Exception {
    String mfaUrl = "/mfa/setup";
    MfaRequiredException exception = new MfaRequiredException("MFA required", mfaUrl);

    failureHandler.onAuthenticationFailure(request, response, exception);

    verify(response).sendRedirect(mfaUrl);
  }

  @Test
  void onAuthenticationFailure_WithContextPath_ShouldRedirectToContextRelativeMfaUrl()
      throws Exception {
    String mfaUrl = "/mfa/setup";
    String contextPath = "/auth";
    when(request.getContextPath()).thenReturn(contextPath);

    MfaRequiredException exception = new MfaRequiredException("MFA required", mfaUrl);

    failureHandler.onAuthenticationFailure(request, response, exception);

    verify(response).sendRedirect("/auth/mfa/setup");
  }

  @Test
  void onAuthenticationFailure_WithOtherException_ShouldDelegateToDefaultHandler()
      throws Exception {
    BadCredentialsException exception = new BadCredentialsException("Invalid password");

    failureHandler.onAuthenticationFailure(request, response, exception);

    // Verify it delegates to the default SimpleUrlAuthenticationFailureHandler
    // Verify sendRedirect was called
    verify(response, atLeastOnce()).sendRedirect(anyString());
  }
}
