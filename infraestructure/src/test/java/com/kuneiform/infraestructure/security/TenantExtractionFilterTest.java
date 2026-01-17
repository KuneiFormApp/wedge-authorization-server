package com.kuneiform.infraestructure.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TenantExtractionFilterTest {

  @Mock private TenantResolver tenantResolver;
  @Mock private FilterChain filterChain;
  @Mock private HttpServletRequest request;
  @Mock private HttpServletResponse response;

  @InjectMocks private TenantExtractionFilter filter;

  @BeforeEach
  void setUp() {
    TenantContext.clear();
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  @Test
  void shouldResolveTenantAndSetContextBeforeChain() throws Exception {
    // Given
    String tenantId = "tenant-123";
    when(tenantResolver.resolveTenant(request)).thenReturn(tenantId);

    // Mock the filter chain to verify context is set when chain is called
    doAnswer(
            invocation -> {
              assertEquals(
                  tenantId,
                  TenantContext.getCurrentTenant(),
                  "Tenant context should be set during chain execution");
              return null;
            })
        .when(filterChain)
        .doFilter(request, response);

    // When
    filter.doFilterInternal(request, response, filterChain);

    // Then
    verify(tenantResolver).resolveTenant(request);
    verify(filterChain).doFilter(request, response);

    // Verify cleanup happened after chain returned
    assertNull(
        TenantContext.getCurrentTenant(), "Tenant context should be cleared after execution");
  }

  @Test
  void shouldClearContextEvenIfChainThrowsException() throws Exception {
    // Given
    String tenantId = "tenant-err";
    when(tenantResolver.resolveTenant(request)).thenReturn(tenantId);

    doAnswer(
            invocation -> {
              assertEquals(tenantId, TenantContext.getCurrentTenant());
              throw new RuntimeException("Chain failed");
            })
        .when(filterChain)
        .doFilter(request, response);

    // When & Then
    assertThrows(
        RuntimeException.class, () -> filter.doFilterInternal(request, response, filterChain));

    // Verify cleanup happened
    assertNull(
        TenantContext.getCurrentTenant(), "Tenant context should be cleared after exception");
  }
}
