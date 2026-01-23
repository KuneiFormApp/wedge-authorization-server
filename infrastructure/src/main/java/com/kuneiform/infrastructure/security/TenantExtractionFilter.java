package com.kuneiform.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter that extracts the tenant ID from the request and sets it in TenantContext. Runs early in
 * the filter chain to ensure tenant is available for all downstream processing.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class TenantExtractionFilter extends OncePerRequestFilter {

  private final TenantResolver tenantResolver;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    try {
      // Resolve and set tenant for this request
      String tenantId = tenantResolver.resolveTenant(request);
      TenantContext.setCurrentTenant(tenantId);

      log.trace(
          "Tenant context set to '{}' for request: {} {}",
          tenantId,
          request.getMethod(),
          request.getRequestURI());

      // Continue filter chain
      filterChain.doFilter(request, response);

    } finally {
      // Always clear tenant context after request completes
      TenantContext.clear();
    }
  }
}
