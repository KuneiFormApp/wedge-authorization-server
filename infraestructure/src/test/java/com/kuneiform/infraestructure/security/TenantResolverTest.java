package com.kuneiform.infraestructure.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.kuneiform.infraestructure.config.properties.WedgeConfigProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TenantResolverTest {

  @Mock private WedgeConfigProperties properties;
  @Mock private WedgeConfigProperties.MultiTenancyConfig multiTenancyConfig;
  @Mock private HttpServletRequest request;

  private TenantResolver resolver;

  @BeforeEach
  void setUp() {
    when(properties.getMultiTenancy()).thenReturn(multiTenancyConfig);

    resolver = new TenantResolver(properties);
  }

  @Test
  void shouldReturnDefaultTenantWhenMultiTenancyDisabled() {
    // Given
    when(multiTenancyConfig.isEnabled()).thenReturn(false);
    when(multiTenancyConfig.getDefaultTenant()).thenReturn("default-tenant");

    // When
    String result = resolver.resolveTenant(request);

    // Then
    assertEquals("default-tenant", result);
    // Request logic should be skipped if disabled
    verifyNoInteractions(request);
  }

  @Test
  void shouldResolveTenantFromPathWhenEnabled() {
    // Given
    when(multiTenancyConfig.isEnabled()).thenReturn(true);
    when(request.getRequestURI()).thenReturn("/tenant-xyz/oauth2/authorize");

    // When
    String result = resolver.resolveTenant(request);

    // Then
    assertEquals("tenant-xyz", result);
  }

  @Test
  void shouldReturnDefaultTenantWhenPathDoesNotMatchPattern() {
    // Given
    when(multiTenancyConfig.isEnabled()).thenReturn(true);
    when(multiTenancyConfig.getDefaultTenant()).thenReturn("default-tenant");
    when(request.getRequestURI()).thenReturn("/login");

    // When
    String result = resolver.resolveTenant(request);

    // Then
    assertEquals("default-tenant", result);
  }

  @Test
  void shouldReturnTrueForHasTenantInPathWhenMatches() {
    // Given
    when(multiTenancyConfig.isEnabled()).thenReturn(true);
    when(request.getRequestURI()).thenReturn("/tenant-123/oauth2/token");

    // When
    boolean result = resolver.hasTenantInPath(request);

    // Then
    assertTrue(result);
  }

  @Test
  void shouldReturnFalseForHasTenantInPathWhenMultiTenancyDisabled() {
    // Given
    when(multiTenancyConfig.isEnabled()).thenReturn(false);

    // When
    boolean result = resolver.hasTenantInPath(request);

    // Then
    assertFalse(result);
  }

  @Test
  void shouldReturnFalseForHasTenantInPathWhenPathDoesNotMatch() {
    // Given
    when(multiTenancyConfig.isEnabled()).thenReturn(true);
    when(request.getRequestURI()).thenReturn("/oauth2/authorize"); // Missing tenant segment

    // When
    boolean result = resolver.hasTenantInPath(request);

    // Then
    assertFalse(result);
  }
}
