package com.kuneiform.infraestructure.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.savedrequest.SavedRequest;

@ExtendWith(MockitoExtension.class)
class MfaAwareAuthenticationFailureHandlerTest {

  @Mock private HttpServletRequest request;
  @Mock private HttpServletResponse response;
  @Mock private HttpSession session;
  @Mock private SavedRequest savedRequest;
  @Mock private SimpleUrlAuthenticationFailureHandler defaultHandler;

  @Test
  void shouldRedirectToMfaVerifictionWhenMfaRequired() throws Exception {
    MfaAwareAuthenticationFailureHandler handler =
        new MfaAwareAuthenticationFailureHandler(defaultHandler);

    String redirectUrl = "/mfa/verify";
    HttpUserAuthenticationProvider.MfaRequiredException exception =
        new HttpUserAuthenticationProvider.MfaRequiredException("MFA Required", redirectUrl);

    when(request.getSession(false)).thenReturn(session);
    when(session.getAttribute("SPRING_SECURITY_SAVED_REQUEST")).thenReturn(savedRequest);
    when(savedRequest.getRedirectUrl()).thenReturn("http://original.url");
    when(request.getContextPath()).thenReturn("/context");

    handler.onAuthenticationFailure(request, response, exception);

    verify(response).sendRedirect("/context/mfa/verify");
    verify(session).setAttribute("MFA_REDIRECT_URL", "http://original.url");
    verify(defaultHandler, never()).onAuthenticationFailure(any(), any(), any());
  }

  @Test
  void shouldDelegateToDefaultHandlerWhenOtherException() throws Exception {
    MfaAwareAuthenticationFailureHandler handler =
        new MfaAwareAuthenticationFailureHandler(defaultHandler);

    BadCredentialsException exception = new BadCredentialsException("Bad credentials");

    handler.onAuthenticationFailure(request, response, exception);

    verify(defaultHandler).onAuthenticationFailure(request, response, exception);
    verify(response, never()).sendRedirect(anyString());
  }

  @Test
  void shouldHandleMfaRequiredWithoutSession() throws Exception {
    MfaAwareAuthenticationFailureHandler handler =
        new MfaAwareAuthenticationFailureHandler(defaultHandler);

    String redirectUrl = "/mfa/setup";
    HttpUserAuthenticationProvider.MfaRequiredException exception =
        new HttpUserAuthenticationProvider.MfaRequiredException("MFA Setup Required", redirectUrl);

    when(request.getSession(false)).thenReturn(null);
    when(request.getContextPath()).thenReturn("/context");

    handler.onAuthenticationFailure(request, response, exception);

    verify(response).sendRedirect("/context/mfa/setup");
    // Verify no session interaction that would throw NPE
    verify(session, never()).setAttribute(anyString(), any());
    verify(defaultHandler, never()).onAuthenticationFailure(any(), any(), any());
  }
}
