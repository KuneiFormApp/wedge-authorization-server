package com.kuneiform.infraestructure.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.kuneiform.application.usecase.AuthenticateUserUseCase;
import com.kuneiform.domain.model.User;
import com.kuneiform.infraestructure.config.properties.WedgeConfigProperties;
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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.savedrequest.DefaultSavedRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@ExtendWith(MockitoExtension.class)
class HttpUserAuthenticationProviderTest {

  @Mock private AuthenticateUserUseCase authenticateUserUseCase;
  @Mock private WedgeConfigProperties config;

  private HttpUserAuthenticationProvider provider;
  private MockHttpServletRequest mockRequest;

  private static final String TEST_CLIENT_ID = "test-client";

  @BeforeEach
  void setUp() {
    // Mock client configuration
    WedgeConfigProperties.ClientConfig clientConfig = new WedgeConfigProperties.ClientConfig();
    clientConfig.setClientId(TEST_CLIENT_ID);
    clientConfig.setTenantId("test-tenant");

    lenient().when(config.getClients()).thenReturn(List.of(clientConfig));

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

    when(authenticateUserUseCase.execute(TEST_CLIENT_ID, "alice", "password123"))
        .thenReturn(Optional.of(user));

    Authentication auth = new UsernamePasswordAuthenticationToken("alice", "password123");
    Authentication result = provider.authenticate(auth);

    assertNotNull(result);
    assertTrue(result.isAuthenticated());

    // Principal should now be the User object
    assertTrue(result.getPrincipal() instanceof User);
    User principalUser = (User) result.getPrincipal();
    assertEquals("alice", principalUser.getUsername());
    assertEquals("user-123", principalUser.getUserId());

    List<String> authorities =
        result.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
    assertTrue(authorities.contains("USER"));
    assertTrue(authorities.contains("ADMIN"));
  }

  @Test
  void shouldRejectInvalidCredentials() {
    when(authenticateUserUseCase.execute(TEST_CLIENT_ID, "alice", "wrongpassword"))
        .thenReturn(Optional.empty());

    Authentication auth = new UsernamePasswordAuthenticationToken("alice", "wrongpassword");

    assertThrows(BadCredentialsException.class, () -> provider.authenticate(auth));
  }

  @Test
  void shouldSupportUsernamePasswordAuthentication() {
    assertTrue(provider.supports(UsernamePasswordAuthenticationToken.class));
  }

  @Test
  void shouldUseAuthoritiesAsProvided() {
    User user =
        User.builder()
            .userId("user-123")
            .username("alice")
            .metadata(Map.of("authorities", List.of("ROLE_USER", "ROLE_ADMIN")))
            .build();

    when(authenticateUserUseCase.execute(TEST_CLIENT_ID, "alice", "password"))
        .thenReturn(Optional.of(user));

    Authentication auth = new UsernamePasswordAuthenticationToken("alice", "password");
    Authentication result = provider.authenticate(auth);

    List<String> authorities =
        result.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();

    // Authorities should be used as provided
    assertTrue(authorities.contains("ROLE_USER"));
    assertTrue(authorities.contains("ROLE_ADMIN"));
  }

  @Test
  void shouldHandleUserWithoutAuthorities() {
    User user =
        User.builder()
            .userId("user-123")
            .username("alice")
            .metadata(Map.of("department", "Engineering"))
            .build();

    when(authenticateUserUseCase.execute(TEST_CLIENT_ID, "alice", "password"))
        .thenReturn(Optional.of(user));

    Authentication auth = new UsernamePasswordAuthenticationToken("alice", "password");
    Authentication result = provider.authenticate(auth);

    // Should still have default ROLE_USER
    List<String> authorities =
        result.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
    assertEquals(1, authorities.size());
    assertTrue(authorities.contains("ROLE_USER"));
  }

  @Test
  void shouldExtractClientIdFromRequestParameter() {
    mockRequest.setParameter("client_id", "my-client");

    User user = User.builder().userId("user-123").username("alice").build();

    when(authenticateUserUseCase.execute("my-client", "alice", "password"))
        .thenReturn(Optional.of(user));

    Authentication auth = new UsernamePasswordAuthenticationToken("alice", "password");
    Authentication result = provider.authenticate(auth);

    assertNotNull(result);
    assertTrue(result.isAuthenticated());
    verify(authenticateUserUseCase).execute("my-client", "alice", "password");
  }

  @Test
  void shouldExtractClientIdFromSavedRequest() {
    // Remove direct parameter
    mockRequest.removeParameter("client_id");

    // Create a saved request with client_id
    MockHttpServletRequest savedReq = new MockHttpServletRequest();
    savedReq.setParameter("client_id", "saved-client");
    DefaultSavedRequest savedRequest = new DefaultSavedRequest(savedReq, null);

    // Add to session
    mockRequest.getSession().setAttribute("SPRING_SECURITY_SAVED_REQUEST", savedRequest);

    User user = User.builder().userId("user-123").username("alice").build();

    when(authenticateUserUseCase.execute("saved-client", "alice", "password"))
        .thenReturn(Optional.of(user));

    Authentication auth = new UsernamePasswordAuthenticationToken("alice", "password");
    Authentication result = provider.authenticate(auth);

    assertNotNull(result);
    assertTrue(result.isAuthenticated());
    verify(authenticateUserUseCase).execute("saved-client", "alice", "password");
  }

  @Test
  void shouldThrowExceptionWhenClientIdNotFound() {
    // Remove client_id parameter
    mockRequest.removeParameter("client_id");

    Authentication auth = new UsernamePasswordAuthenticationToken("alice", "password");

    BadCredentialsException exception =
        assertThrows(BadCredentialsException.class, () -> provider.authenticate(auth));

    assertTrue(exception.getMessage().contains("client_id is required"));
  }

  @Test
  void shouldThrowExceptionWhenNoRequestContext() {
    // Clear request context
    RequestContextHolder.resetRequestAttributes();

    Authentication auth = new UsernamePasswordAuthenticationToken("alice", "password");

    BadCredentialsException exception =
        assertThrows(BadCredentialsException.class, () -> provider.authenticate(auth));

    assertTrue(exception.getMessage().contains("No request context available"));
  }

  @Test
  void shouldExtractClientIdFromSavedRequestStringFallback() {
    // Remove direct parameter
    mockRequest.removeParameter("client_id");

    // Create a mock saved request object (not a SavedRequest instance)
    Object savedRequest =
        new Object() {
          @Override
          public String toString() {
            return "SavedRequestImpl[url=http://localhost/oauth2/authorize?client_id=fallback-client&scope=openid]";
          }
        };

    mockRequest.getSession().setAttribute("SPRING_SECURITY_SAVED_REQUEST", savedRequest);

    User user = User.builder().userId("user-123").username("alice").build();

    when(authenticateUserUseCase.execute("fallback-client", "alice", "password"))
        .thenReturn(Optional.of(user));

    Authentication auth = new UsernamePasswordAuthenticationToken("alice", "password");
    Authentication result = provider.authenticate(auth);

    assertNotNull(result);
    assertTrue(result.isAuthenticated());
    verify(authenticateUserUseCase).execute("fallback-client", "alice", "password");
  }
}
