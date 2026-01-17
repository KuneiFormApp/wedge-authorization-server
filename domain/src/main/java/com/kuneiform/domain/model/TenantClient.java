package com.kuneiform.domain.model;

import lombok.Builder;
import lombok.Value;

/**
 * Domain model representing the relationship between a tenant and an OAuth client. Allows one
 * client to be associated with multiple tenants (many-to-many).
 */
@Value
@Builder
public class TenantClient {
  /** Tenant identifier */
  String tenantId;

  /** OAuth client identifier */
  String clientId;
}
