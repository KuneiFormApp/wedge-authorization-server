package com.kuneiform.infrastructure.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.kuneiform.application.usecase.AuthenticateUserUseCase;
import com.kuneiform.domain.model.User;
import com.kuneiform.infrastructure.config.properties.WedgeConfigProperties;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@ExtendWith(MockitoExtension.class)
class HttpUserAuthenticationProviderTest {

  @Mock private AuthenticateUserUseCase authenticateUserUseCase;
  @Mock private WedgeConfigProperties config;
  @Mock private WedgeConfigProperties.LoginConfig loginConfig;

  private HttpUserAuthenticationProvider provider;
  private MockHttpServletRequest mockRequest;

  private static final String TEST_CLIENT_ID = "test-client";

  @BeforeEach
  void setUp() {
    // Mock login config to return null/empty by default
    lenient().when(config.getLogin()).thenReturn(loginConfig);
    lenient().when(loginConfig.getDefaultClientId()).thenReturn(null);
    lenient().when(config.getTenants()).thenReturn(Collections.emptyList());

    provider = new HttpUserAuthenticationProvider(authenticateUserUseCase, config);

    // Setup mock request context
    mockRequest = new MockHttpServletRequest();
    mockRequest.setParameter("client_id", TEST_CLIENT_ID);
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(mockRequest));
  }

  @AfterEach
  void tearDown() {
    RequestContextHolder.resetRequestAttributes();
  }

  @Test
  void shouldAuthenticateValidUser() {
    User user =
        User.builder()
            .userId("user-123")
            .username("alice")
            .email("alice@example.com")
            .metadata(Map.of("authorities", List.of("USER", "ADMIN")))
            .build();

    when(authenticateUserUseCase.execute(
            eq(TEST_CLIENT_ID), isNull(), eq("alice"), eq("password123"), any()))
        .thenReturn(Optional.of(user));

    Authentication auth = new UsernamePasswordAuthenticationToken("alice", "password123");
    Authentication result = provider.authenticate(auth);

    assertNotNull(result);
    assertTrue(result.isAuthenticated());
    assertTrue(result.getPrincipal() instanceof User);
  }

  @Test
  void shouldRejectInvalidCredentials() {
    when(authenticateUserUseCase.execute(
            eq(TEST_CLIENT_ID), isNull(), eq("alice"), eq("wrongpassword"), any()))
        .thenReturn(Optional.empty());

    Authentication auth = new UsernamePasswordAuthenticationToken("alice", "wrongpassword");

    assertThrows(BadCredentialsException.class, () -> provider.authenticate(auth));
  }

  @Test
  void shouldExtractClientIdFromRequestParameter() {
    mockRequest.setParameter("client_id", "my-client");

    User user = User.builder().userId("user-123").username("alice").build();

    when(authenticateUserUseCase.execute(
            eq("my-client"), isNull(), eq("alice"), eq("password"), any()))
        .thenReturn(Optional.of(user));

    Authentication auth = new UsernamePasswordAuthenticationToken("alice", "password");
    Authentication result = provider.authenticate(auth);

    assertTrue(result.isAuthenticated());
    verify(authenticateUserUseCase)
        .execute(eq("my-client"), isNull(), eq("alice"), eq("password"), any());
  }

  @Test
  void shouldFallbackToDefaultClientIdWhenNoClientIdInRequest() {
    // Remove direct parameter
    mockRequest.removeParameter("client_id");

    // Configure default client ID
    when(loginConfig.getDefaultClientId()).thenReturn("default-app");

    User user = User.builder().userId("user-123").username("alice").build();
    when(authenticateUserUseCase.execute(
            eq("default-app"), isNull(), eq("alice"), eq("password"), any()))
        .thenReturn(Optional.of(user));

    Authentication auth = new UsernamePasswordAuthenticationToken("alice", "password");
    Authentication result = provider.authenticate(auth);

    assertTrue(result.isAuthenticated());
    verify(authenticateUserUseCase)
        .execute(eq("default-app"), isNull(), eq("alice"), eq("password"), any());
  }

  @Test
  void shouldFallbackToDefaultTenantIdWhenNoClientAndNoDefaultClient() {
    // Remove direct parameter
    mockRequest.removeParameter("client_id");

    // Ensure default client is null
    when(loginConfig.getDefaultClientId()).thenReturn(null);
    // Ensure tenants list is empty (falls back to "default-tenant")
    when(config.getTenants()).thenReturn(Collections.emptyList());

    User user = User.builder().userId("user-123").username("alice").build();

    // It should call execute with null clientId and "default-tenant" as tenantId
    when(authenticateUserUseCase.execute(
            isNull(), eq("default-tenant"), eq("alice"), eq("password"), any()))
        .thenReturn(Optional.of(user));

    Authentication auth = new UsernamePasswordAuthenticationToken("alice", "password");
    Authentication result = provider.authenticate(auth);

    assertTrue(result.isAuthenticated());
    verify(authenticateUserUseCase)
        .execute(isNull(), eq("default-tenant"), eq("alice"), eq("password"), any());
  }
}
