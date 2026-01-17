package com.kuneiform.infraestructure.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.kuneiform.application.usecase.GenerateMfaQrCodeUseCase;
import com.kuneiform.application.usecase.GenerateMfaSecretUseCase;
import com.kuneiform.application.usecase.RegisterMfaWithProviderUseCase;
import com.kuneiform.application.usecase.ValidateMfaTotpUseCase;
import com.kuneiform.boot.WedgeAuthorizationServerStarter;
import com.kuneiform.domain.model.MfaData;
import com.kuneiform.domain.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Integration tests for MFA (Multi-Factor Authentication) flow.
 *
 * <p>Tests the complete MFA setup and verification flows including: - MFA setup page rendering - QR
 * code generation - TOTP code verification during setup - MFA verification page rendering - TOTP
 * code verification during login
 */
@SpringBootTest(classes = WedgeAuthorizationServerStarter.class)
@Import(MfaFlowIT.MockMvcConfig.class)
class MfaFlowIT {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private GenerateMfaSecretUseCase generateMfaSecretUseCase;
  @MockitoBean private GenerateMfaQrCodeUseCase generateMfaQrCodeUseCase;
  @MockitoBean private ValidateMfaTotpUseCase validateMfaTotpUseCase;
  @MockitoBean private RegisterMfaWithProviderUseCase registerMfaWithProviderUseCase;

  /** Test that MFA setup page is accessible and displays the expected content. */
  @Test
  void mfaSetup_shouldRenderSetupPage() throws Exception {
    // Given: User is in MFA setup flow with pending user in session
    MockHttpSession session = createMfaSetupSession();
    String testSecret = "JBSWY3DPEHPK3PXP";
    when(generateMfaSecretUseCase.execute()).thenReturn(testSecret);

    // When: User accesses MFA setup page
    mockMvc
        .perform(get("/mfa/setup").session(session))
        // Then: Setup page is displayed
        .andExpect(status().isOk())
        .andExpect(view().name("mfa-setup"))
        .andExpect(content().string(containsString("Set Up Two-Factor Authentication")))
        .andExpect(content().string(containsString(testSecret)));

    // Verify secret was generated
    verify(generateMfaSecretUseCase).execute();
  }

  /** Test that MFA setup page is accessible in Spanish locale. */
  @Test
  void mfaSetup_shouldRenderSetupPageInSpanish() throws Exception {
    // Given: User is in MFA setup flow
    MockHttpSession session = createMfaSetupSession();
    String testSecret = "JBSWY3DPEHPK3PXP";
    when(generateMfaSecretUseCase.execute()).thenReturn(testSecret);

    // When: User accesses MFA setup page with Spanish locale
    mockMvc
        .perform(get("/mfa/setup").param("lang", "es").session(session))
        // Then: Setup page is displayed in Spanish
        .andExpect(status().isOk())
        .andExpect(view().name("mfa-setup"))
        .andExpect(content().string(containsString("Configurar Autenticación de Dos Factores")));
  }

  /** Test that MFA setup redirects to login if no pending user in session. */
  @Test
  void mfaSetup_shouldRedirectToLoginWhenNoPendingUser() throws Exception {
    // Given: No pending user in session
    MockHttpSession session = new MockHttpSession();

    // When: User tries to access MFA setup
    mockMvc
        .perform(get("/mfa/setup").session(session))
        // Then: Redirected to login
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login"));
  }

  /** Test QR code image generation. */
  @Test
  void mfaQrCode_shouldGenerateQrCodeImage() throws Exception {
    // Given: User is in MFA setup flow with secret in session
    MockHttpSession session = createMfaSetupSession();
    String testSecret = "JBSWY3DPEHPK3PXP";
    session.setAttribute("MFA_SECRET", testSecret);
    session.setAttribute("MFA_KEY_ID", "WedgeAuth:test@example.com");

    byte[] mockQrCode = new byte[] {1, 2, 3, 4}; // Mock QR code bytes
    when(generateMfaQrCodeUseCase.execute(anyString(), anyString(), anyString(), eq(300), eq(300)))
        .thenReturn(mockQrCode);

    // When: User requests QR code image
    mockMvc
        .perform(get("/mfa/qrcode").session(session))
        // Then: QR code image is returned
        .andExpect(status().isOk())
        .andExpect(content().contentType("image/png"));

    // Verify QR code was generated
    verify(generateMfaQrCodeUseCase)
        .execute(anyString(), anyString(), anyString(), eq(300), eq(300));
  }

  /** Test QR code generation redirects to setup if no secret in session. */
  @Test
  void mfaQrCode_shouldRedirectToSetupWhenNoSecret() throws Exception {
    // Given: No secret in session
    MockHttpSession session = createMfaSetupSession();

    // When: User tries to access QR code
    mockMvc
        .perform(get("/mfa/qrcode").session(session))
        // Then: Redirected to setup
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/mfa/setup"));
  }

  /** Test successful MFA setup verification with valid TOTP code. */
  @Test
  void mfaSetupVerify_shouldCompleteSetupWithValidCode() throws Exception {
    // Given: User is in MFA setup flow
    MockHttpSession session = createMfaSetupSession();
    String testSecret = "JBSWY3DPEHPK3PXP";
    session.setAttribute("MFA_SECRET", testSecret);
    session.setAttribute("MFA_KEY_ID", "WedgeAuth:test@example.com");

    String validCode = "123456";
    when(validateMfaTotpUseCase.execute(testSecret, validCode)).thenReturn(true);
    when(registerMfaWithProviderUseCase.execute(anyString(), anyString(), anyString(), anyString()))
        .thenReturn(true);

    // When: User submits valid TOTP code
    MvcResult result =
        mockMvc
            .perform(
                post("/mfa/setup/verify")
                    .param("totpCode", validCode)
                    .session(session)
                    .with(csrf()))
            // Then: Setup is completed and redirected to login
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"))
            .andReturn();

    // Verify MFA was registered
    verify(validateMfaTotpUseCase).execute(testSecret, validCode);
    verify(registerMfaWithProviderUseCase)
        .execute(eq("test-client"), eq("test-user"), eq(testSecret), anyString());

    // Verify MFA_VERIFIED flag is set
    MockHttpSession resultSession = (MockHttpSession) result.getRequest().getSession();
    assert resultSession != null;
    assert Boolean.TRUE.equals(resultSession.getAttribute("MFA_VERIFIED"));
  }

  /** Test MFA setup verification fails with invalid TOTP code. */
  @Test
  void mfaSetupVerify_shouldFailWithInvalidCode() throws Exception {
    // Given: User is in MFA setup flow
    MockHttpSession session = createMfaSetupSession();
    String testSecret = "JBSWY3DPEHPK3PXP";
    session.setAttribute("MFA_SECRET", testSecret);
    session.setAttribute("MFA_KEY_ID", "WedgeAuth:test@example.com");

    String invalidCode = "000000";
    when(validateMfaTotpUseCase.execute(testSecret, invalidCode)).thenReturn(false);

    // When: User submits invalid TOTP code
    mockMvc
        .perform(
            post("/mfa/setup/verify").param("totpCode", invalidCode).session(session).with(csrf()))
        // Then: Redirected back to setup with error
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/mfa/setup?error"));

    // Verify code was validated but not registered
    verify(validateMfaTotpUseCase).execute(testSecret, invalidCode);
    verify(registerMfaWithProviderUseCase, org.mockito.Mockito.never())
        .execute(anyString(), anyString(), anyString(), anyString());
  }

  /** Test MFA verify page is accessible and displays the expected content. */
  @Test
  void mfaVerify_shouldRenderVerifyPage() throws Exception {
    // Given: User is in MFA verification flow
    MockHttpSession session = createMfaVerifySession();

    // When: User accesses MFA verify page
    mockMvc
        .perform(get("/mfa/verify").session(session))
        // Then: Verify page is displayed
        .andExpect(status().isOk())
        .andExpect(view().name("mfa-verify"))
        .andExpect(content().string(containsString("Two-Factor Authentication")))
        .andExpect(content().string(containsString("Enter the 6-digit code")));
  }

  /** Test MFA verify page is accessible in Spanish locale. */
  @Test
  void mfaVerify_shouldRenderVerifyPageInSpanish() throws Exception {
    // Given: User is in MFA verification flow
    MockHttpSession session = createMfaVerifySession();

    // When: User accesses MFA verify page with Spanish locale
    mockMvc
        .perform(get("/mfa/verify").param("lang", "es").session(session))
        // Then: Verify page is displayed in Spanish
        .andExpect(status().isOk())
        .andExpect(view().name("mfa-verify"))
        .andExpect(content().string(containsString("Autenticación de Dos Factores")));
  }

  /** Test MFA verify redirects to login if no pending user in session. */
  @Test
  void mfaVerify_shouldRedirectToLoginWhenNoPendingUser() throws Exception {
    // Given: No pending user in session
    MockHttpSession session = new MockHttpSession();

    // When: User tries to access MFA verify
    mockMvc
        .perform(get("/mfa/verify").session(session))
        // Then: Redirected to login
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login"));
  }

  /** Test successful MFA verification with valid TOTP code. */
  @Test
  void mfaVerifyPost_shouldCompleteVerificationWithValidCode() throws Exception {
    // Given: User is in MFA verification flow with registered MFA
    MockHttpSession session = createMfaVerifySession();
    User user = (User) session.getAttribute("MFA_PENDING_USER");
    String userSecret = user.getMfaData().getMfaSecret();

    String validCode = "654321";
    when(validateMfaTotpUseCase.execute(userSecret, validCode)).thenReturn(true);

    // When: User submits valid TOTP code
    MvcResult result =
        mockMvc
            .perform(post("/mfa/verify").param("totpCode", validCode).session(session).with(csrf()))
            // Then: Verification is completed and redirected to login
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"))
            .andReturn();

    // Verify code was validated
    verify(validateMfaTotpUseCase).execute(userSecret, validCode);

    // Verify MFA_VERIFIED flag is set
    MockHttpSession resultSession = (MockHttpSession) result.getRequest().getSession();
    assert resultSession != null;
    assert Boolean.TRUE.equals(resultSession.getAttribute("MFA_VERIFIED"));
  }

  /** Test MFA verification fails with invalid TOTP code. */
  @Test
  void mfaVerifyPost_shouldFailWithInvalidCode() throws Exception {
    // Given: User is in MFA verification flow
    MockHttpSession session = createMfaVerifySession();
    User user = (User) session.getAttribute("MFA_PENDING_USER");
    String userSecret = user.getMfaData().getMfaSecret();

    String invalidCode = "999999";
    when(validateMfaTotpUseCase.execute(userSecret, invalidCode)).thenReturn(false);

    // When: User submits invalid TOTP code
    mockMvc
        .perform(post("/mfa/verify").param("totpCode", invalidCode).session(session).with(csrf()))
        // Then: Redirected back to verify with error
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/mfa/verify?error"));

    // Verify code was validated
    verify(validateMfaTotpUseCase).execute(userSecret, invalidCode);
  }

  /** Test MFA verification fails when user has no MFA secret. */
  @Test
  void mfaVerifyPost_shouldFailWhenNoMfaSecret() throws Exception {
    // Given: User in session but no MFA secret
    MockHttpSession session = new MockHttpSession();
    User userWithoutSecret =
        User.builder()
            .userId("test-user")
            .username("test@example.com")
            .mfaEnabled(true)
            .mfaData(
                MfaData.builder()
                    .twoFaRegistered(true)
                    .mfaKeyId("WedgeAuth:test@example.com")
                    .mfaSecret(null) // No secret!
                    .build())
            .build();
    session.setAttribute("MFA_PENDING_USER", userWithoutSecret);
    session.setAttribute("CLIENT_ID", "test-client");

    // When: User tries to verify TOTP
    mockMvc
        .perform(post("/mfa/verify").param("totpCode", "123456").session(session).with(csrf()))
        // Then: Redirected to login with error
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login?error"));
  }

  /**
   * Helper method to create a session for MFA setup flow.
   *
   * @return MockHttpSession with MFA_PENDING_USER and CLIENT_ID
   */
  private MockHttpSession createMfaSetupSession() {
    MockHttpSession session = new MockHttpSession();
    User user =
        User.builder()
            .userId("test-user")
            .username("test@example.com")
            .email("test@example.com")
            .mfaEnabled(true)
            .mfaData(
                MfaData.builder()
                    .twoFaRegistered(false) // Not yet registered
                    .mfaKeyId("WedgeAuth:test@example.com")
                    .mfaSecret(null)
                    .build())
            .build();
    session.setAttribute("MFA_PENDING_USER", user);
    session.setAttribute("CLIENT_ID", "test-client");
    return session;
  }

  /**
   * Helper method to create a session for MFA verification flow.
   *
   * @return MockHttpSession with MFA_PENDING_USER (with registered MFA) and CLIENT_ID
   */
  private MockHttpSession createMfaVerifySession() {
    MockHttpSession session = new MockHttpSession();
    User user =
        User.builder()
            .userId("test-user")
            .username("test@example.com")
            .email("test@example.com")
            .mfaEnabled(true)
            .mfaData(
                MfaData.builder()
                    .twoFaRegistered(true) // Already registered
                    .mfaKeyId("WedgeAuth:test@example.com")
                    .mfaSecret("JBSWY3DPEHPK3PXP") // Has secret
                    .build())
            .build();
    session.setAttribute("MFA_PENDING_USER", user);
    session.setAttribute("CLIENT_ID", "test-client");
    return session;
  }

  @TestConfiguration
  static class MockMvcConfig {
    @Bean
    public MockMvc mockMvc(WebApplicationContext context) {
      return MockMvcBuilders.webAppContextSetup(context)
          .apply(SecurityMockMvcConfigurers.springSecurity())
          .build();
    }
  }
}
