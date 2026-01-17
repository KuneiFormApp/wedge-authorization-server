package com.kuneiform.infraestructure.adapter;

import com.kuneiform.domain.model.Tenant;
import com.kuneiform.domain.model.UserProvider;
import com.kuneiform.domain.port.TenantRepository;
import com.kuneiform.infraestructure.persistence.entity.TenantEntity;
import com.kuneiform.infraestructure.persistence.repository.TenantJdbcRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;

/**
 * Database-backed implementation of TenantRepository using Spring Data JDBC. Includes an in-memory
 * cache for improved performance.
 */
@Slf4j
public class DatabaseTenantRepositoryAdapter implements TenantRepository {

  private final TenantJdbcRepository repository;
  private final Map<String, Tenant> cache = new ConcurrentHashMap<>();

  public DatabaseTenantRepositoryAdapter(TenantJdbcRepository repository) {
    this.repository = repository;
    loadCache();
  }

  /** Loads all tenants from the database into the cache on startup. */
  private void loadCache() {
    repository
        .findAll()
        .forEach(
            entity -> {
              Tenant tenant = toTenant(entity);
              cache.put(tenant.getId(), tenant);
              log.info(
                  "Loaded tenant from database: {} ({}) with user provider: {}",
                  tenant.getId(),
                  tenant.getName(),
                  tenant.getUserProvider().getEndpoint());
            });
    log.info("Loaded {} tenants from database into cache", cache.size());
  }

  @Override
  public Optional<Tenant> findById(String tenantId) {
    // Check cache first
    Tenant cachedTenant = cache.get(tenantId);
    if (cachedTenant != null) {
      return Optional.of(cachedTenant);
    }

    // Fall back to database if not in cache
    return repository.findById(tenantId).map(this::toTenant);
  }

  @Override
  public List<Tenant> findAll() {
    return new ArrayList<>(cache.values());
  }

  /** Converts database entity to domain model. */
  private Tenant toTenant(TenantEntity entity) {
    UserProvider userProvider =
        UserProvider.builder()
            .endpoint(entity.getUserProviderEndpoint())
            .timeout(
                entity.getUserProviderTimeout() != null ? entity.getUserProviderTimeout() : 5000)
            .mfaRegistrationEndpoint(entity.getMfaRegistrationEndpoint())
            .build();

    return Tenant.builder()
        .id(entity.getId())
        .name(entity.getName())
        .userProvider(userProvider)
        .build();
  }
}
