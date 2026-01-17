package com.kuneiform.infraestructure.security;

import com.kuneiform.infraestructure.config.properties.WedgeConfigProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service responsible for resolving the tenant ID from HTTP requests. Supports path-based tenancy:
 * /{tenant}/oauth2/authorize
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantResolver {

  private final WedgeConfigProperties wedgeConfig;

  // Pattern to extract tenant from path: /{tenant}/oauth2/...
  private static final Pattern TENANT_PATH_PATTERN = Pattern.compile("^/([^/]+)/oauth2/.*");

  /**
   * Returns the tenant ID, or default tenant if multi-tenancy is disabled or path doesn't match.
   */
  public String resolveTenant(HttpServletRequest request) {
    // If multi-tenancy is disabled, always return default tenant
    if (!wedgeConfig.getMultiTenancy().isEnabled()) {
      log.debug(
          "Multi-tenancy disabled, using default tenant: {}",
          wedgeConfig.getMultiTenancy().getDefaultTenant());
      return wedgeConfig.getMultiTenancy().getDefaultTenant();
    }

    // Extract tenant from request path
    String path = request.getRequestURI();
    Matcher matcher = TENANT_PATH_PATTERN.matcher(path);

    if (matcher.matches()) {
      String tenantId = matcher.group(1);
      log.debug("Resolved tenant '{}' from path: {}", tenantId, path);
      return tenantId;
    }

    // Fallback to default tenant if path doesn't match expected pattern
    log.debug(
        "Could not resolve tenant from path '{}', using default: {}",
        path,
        wedgeConfig.getMultiTenancy().getDefaultTenant());
    return wedgeConfig.getMultiTenancy().getDefaultTenant();
  }

  public boolean hasTenantInPath(HttpServletRequest request) {
    if (!wedgeConfig.getMultiTenancy().isEnabled()) {
      return false;
    }

    String path = request.getRequestURI();
    return TENANT_PATH_PATTERN.matcher(path).matches();
  }
}
