package com.kuneiform.infraestructure.persistence.repository;

import com.kuneiform.infraestructure.persistence.entity.TenantEntity;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JDBC repository for tenant entities. Provides CRUD operations for the tenants table.
 */
@Repository
public interface TenantJdbcRepository extends CrudRepository<TenantEntity, String> {

  /**
   * Find a tenant by its ID.
   *
   * @param id the tenant ID
   * @return Optional containing the tenant entity if found
   */
  Optional<TenantEntity> findById(String id);
}
