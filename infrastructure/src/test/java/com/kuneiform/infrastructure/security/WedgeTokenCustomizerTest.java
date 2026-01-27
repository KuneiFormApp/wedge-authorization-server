package com.kuneiform.infrastructure.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.kuneiform.domain.model.User;
import com.kuneiform.domain.model.UserDevice;
import com.kuneiform.domain.port.DeviceStoragePort;
import com.kuneiform.infrastructure.service.DeviceFingerprintService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@ExtendWith(MockitoExtension.class)
class WedgeTokenCustomizerTest {

  @Mock private DeviceStoragePort deviceStorage;
  @Mock private DeviceFingerprintService fingerprintService;
  @Mock private JwtEncodingContext context;
  @Mock private Authentication authentication;
  @Mock private JwtClaimsSet.Builder claimsBuilder;
  @Mock private OAuth2Authorization authorization;

  private WedgeTokenCustomizer customizer;
  private MockedStatic<RequestContextHolder> requestContextMock;

  @BeforeEach
  void setUp() {
    customizer = new WedgeTokenCustomizer(deviceStorage, fingerprintService);
    requestContextMock = mockStatic(RequestContextHolder.class);
  }

  @AfterEach
  void tearDown() {
    requestContextMock.close();
  }

  @Test
  void shouldCustomizeClaimsForUser() {
    // Given
    User user =
        User.builder()
            .userId("user-123")
            .username("testmsg")
            .email("test@example.com")
            .metadata(Map.of("role", "admin"))
            .build();

    when(context.getPrincipal()).thenReturn(authentication);
    when(authentication.getPrincipal()).thenReturn(user);
    when(context.getTokenType()).thenReturn(OAuth2TokenType.ACCESS_TOKEN);
    when(context.getClaims()).thenReturn(claimsBuilder);

    // When
    customizer.customize(context);

    // Then
    ArgumentCaptor<Consumer<Map<String, Object>>> captor = ArgumentCaptor.forClass(Consumer.class);
    verify(claimsBuilder).claims(captor.capture());

    Map<String, Object> claims = new HashMap<>();
    captor.getValue().accept(claims);

    assertEquals("user-123", claims.get("sub"));
    assertEquals("testmsg", claims.get("username"));
    assertEquals("test@example.com", claims.get("email"));
    assertEquals("admin", claims.get("role"));
  }

  @Test
  void shouldSkipCustomizationIfPrincipalIsNotUser() {
    // Given
    when(context.getPrincipal()).thenReturn(authentication);
    when(authentication.getPrincipal()).thenReturn("just-a-string-principal");
    when(context.getTokenType()).thenReturn(OAuth2TokenType.ACCESS_TOKEN);

    // When
    customizer.customize(context);

    // Then
    verify(context, never()).getClaims();
    verifyNoInteractions(deviceStorage);
  }

  @Test
  void shouldTrackDeviceOnAccessTokenIssue() {
    // Given
    User user = User.builder().userId("user-123").username("testuser").build();

    when(context.getPrincipal()).thenReturn(authentication);
    when(authentication.getPrincipal()).thenReturn(user);
    when(context.getTokenType()).thenReturn(OAuth2TokenType.ACCESS_TOKEN);

    // Mock Claims calls (needed to avoid NPE if not mocked, though we don't care about result here)
    lenient().when(context.getClaims()).thenReturn(claimsBuilder);

    // Mock Request Context
    ServletRequestAttributes attributes = mock(ServletRequestAttributes.class);
    HttpServletRequest request = mock(HttpServletRequest.class);
    requestContextMock.when(RequestContextHolder::getRequestAttributes).thenReturn(attributes);
    when(attributes.getRequest()).thenReturn(request);
    when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");

    // Mock Fingerprint Service
    when(fingerprintService.generateDeviceId(any(), any())).thenReturn("device-hash");
    when(fingerprintService.parseDeviceName(any())).thenReturn("Test Browser");
    when(fingerprintService.extractIpAddress(any())).thenReturn("127.0.0.1");

    // Mock Authorization ID extraction
    when(context.getAuthorization()).thenReturn(authorization);
    when(authorization.getId()).thenReturn("auth-id-123");

    // When
    customizer.customize(context);

    // Then
    ArgumentCaptor<UserDevice> deviceCaptor = ArgumentCaptor.forClass(UserDevice.class);
    verify(deviceStorage).save(deviceCaptor.capture());

    UserDevice savedDevice = deviceCaptor.getValue();
    assertEquals("device-hash", savedDevice.getDeviceId());
    assertEquals("user-123", savedDevice.getUserId());
    assertEquals("auth-id-123", savedDevice.getAuthorizationId());
    assertEquals("127.0.0.1", savedDevice.getIpAddress());
  }

  @Test
  void shouldNotTrackDeviceIfUserAgentMissing() {
    // Given
    User user = User.builder().userId("user-123").username("testuser").build();

    when(context.getPrincipal()).thenReturn(authentication);
    when(authentication.getPrincipal()).thenReturn(user);
    // Access Token triggers tracking
    when(context.getTokenType()).thenReturn(OAuth2TokenType.ACCESS_TOKEN);
    lenient().when(context.getClaims()).thenReturn(claimsBuilder);

    // Mock Request without User-Agent
    ServletRequestAttributes attributes = mock(ServletRequestAttributes.class);
    HttpServletRequest request = mock(HttpServletRequest.class);
    requestContextMock.when(RequestContextHolder::getRequestAttributes).thenReturn(attributes);
    when(attributes.getRequest()).thenReturn(request);
    when(request.getHeader("User-Agent")).thenReturn(null);

    // When
    customizer.customize(context);

    // Then
    verifyNoInteractions(deviceStorage);
    verifyNoInteractions(fingerprintService);
  }

  @Test
  void shouldNotTrackDeviceForIdToken() {
    // Given
    User user = User.builder().userId("user-123").username("testuser").build();

    when(context.getPrincipal()).thenReturn(authentication);
    when(authentication.getPrincipal()).thenReturn(user);
    // ID Token should NOT trigger tracking
    when(context.getTokenType()).thenReturn(new OAuth2TokenType(OidcParameterNames.ID_TOKEN));
    when(context.getClaims()).thenReturn(claimsBuilder);

    // When
    customizer.customize(context);

    // Then
    verifyNoInteractions(deviceStorage);
    verify(claimsBuilder).claims(any()); // But claims should still be customized
  }
}
