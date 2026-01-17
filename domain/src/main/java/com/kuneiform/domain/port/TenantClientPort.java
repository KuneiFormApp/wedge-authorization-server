package com.kuneiform.domain.port;

import java.util.List;

/**
 * Port interface for managing tenant-client relationships. Supports querying which clients belong
 * to which tenants.
 */
public interface TenantClientPort {

  /**
   * Get all client IDs associated with a specific tenant.
   *
   * @param tenantId The tenant identifier
   * @return List of client IDs for this tenant
   */
  List<String> getClientIdsForTenant(String tenantId);

  /**
   * Check if a client is associated with a tenant.
   *
   * @param tenantId The tenant identifier
   * @param clientId The client identifier
   * @return true if the client belongs to this tenant
   */
  boolean isClientInTenant(String tenantId, String clientId);

  /**
   * Add a client to a tenant.
   *
   * @param tenantId The tenant identifier
   * @param clientId The client identifier
   */
  void addClientToTenant(String tenantId, String clientId);

  /**
   * Remove a client from a tenant.
   *
   * @param tenantId The tenant identifier
   * @param clientId The client identifier
   */
  void removeClientFromTenant(String tenantId, String clientId);
}
