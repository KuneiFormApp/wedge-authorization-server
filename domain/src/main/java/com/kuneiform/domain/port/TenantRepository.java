package com.kuneiform.domain.port;

import com.kuneiform.domain.model.Tenant;
import java.util.List;
import java.util.Optional;

/** Repository port for managing tenants and their user providers. */
public interface TenantRepository {
  /**
   * Find a tenant by its ID.
   *
   * @param tenantId the tenant ID
   * @return Optional containing the tenant if found
   */
  Optional<Tenant> findById(String tenantId);

  /**
   * Find all tenants.
   *
   * @return list of all tenants
   */
  List<Tenant> findAll();
}
