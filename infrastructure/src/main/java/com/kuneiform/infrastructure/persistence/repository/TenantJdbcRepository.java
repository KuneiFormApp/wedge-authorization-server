package com.kuneiform.infrastructure.persistence.repository;

import com.kuneiform.infrastructure.persistence.entity.TenantEntity;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JDBC repository for tenant entities. Provides CRUD operations for the tenants table.
 */
@Repository
public interface TenantJdbcRepository extends CrudRepository<TenantEntity, String> {

  Optional<TenantEntity> findById(String id);
}
