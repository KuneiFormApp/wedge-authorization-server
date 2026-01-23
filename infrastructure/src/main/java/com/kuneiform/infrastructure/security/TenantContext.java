package com.kuneiform.infrastructure.security;

import lombok.extern.slf4j.Slf4j;

/**
 * Thread-local storage for the current tenant context. Stores the tenant ID for the duration of the
 * current request.
 */
@Slf4j
public class TenantContext {

  private static final ThreadLocal<String> currentTenant = new ThreadLocal<>();

  public static void setCurrentTenant(String tenantId) {
    log.debug("Setting tenant context: {}", tenantId);
    currentTenant.set(tenantId);
  }

  public static String getCurrentTenant() {
    return currentTenant.get();
  }

  /** Should be called after request processing is complete. */
  public static void clear() {
    log.debug("Clearing tenant context");
    currentTenant.remove();
  }

  public static boolean hasTenant() {
    return currentTenant.get() != null;
  }
}
