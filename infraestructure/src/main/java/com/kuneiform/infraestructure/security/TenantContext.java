package com.kuneiform.infraestructure.security;

import lombok.extern.slf4j.Slf4j;

/**
 * Thread-local storage for the current tenant context. Stores the tenant ID for the duration of the
 * current request.
 */
@Slf4j
public class TenantContext {

  private static final ThreadLocal<String> currentTenant = new ThreadLocal<>();

  /**
   * Set the current tenant ID for this thread.
   *
   * @param tenantId The tenant ID to set
   */
  public static void setCurrentTenant(String tenantId) {
    log.debug("Setting tenant context: {}", tenantId);
    currentTenant.set(tenantId);
  }

  /**
   * Get the current tenant ID for this thread.
   *
   * @return The current tenant ID, or null if not set
   */
  public static String getCurrentTenant() {
    return currentTenant.get();
  }

  /**
   * Clear the tenant context for this thread. Should be called after request processing is
   * complete.
   */
  public static void clear() {
    log.debug("Clearing tenant context");
    currentTenant.remove();
  }

  /**
   * Check if a tenant is currently set.
   *
   * @return true if tenant is set, false otherwise
   */
  public static boolean hasTenant() {
    return currentTenant.get() != null;
  }
}
