package com.kuneiform.infrastructure.adapter;

import com.kuneiform.domain.model.Tenant;
import com.kuneiform.domain.model.UserProvider;
import com.kuneiform.domain.port.TenantRepository;
import com.kuneiform.infrastructure.config.properties.WedgeConfigProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;

/** YAML-based implementation of TenantRepository that loads tenants from application.yaml. */
@Slf4j
public class YamlTenantRepositoryAdapter implements TenantRepository {

  private final Map<String, Tenant> tenants = new ConcurrentHashMap<>();

  public YamlTenantRepositoryAdapter(WedgeConfigProperties config) {
    loadTenants(config);
  }

  private void loadTenants(WedgeConfigProperties config) {
    config
        .getTenants()
        .forEach(
            tenantConfig -> {
              UserProvider provider =
                  UserProvider.builder()
                      .endpoint(tenantConfig.getUserProvider().getEndpoint())
                      .timeout(tenantConfig.getUserProvider().getTimeout())
                      .mfaRegistrationEndpoint(
                          tenantConfig.getUserProvider().getMfaRegistrationEndpoint())
                      .build();

              Tenant tenant =
                  Tenant.builder()
                      .id(tenantConfig.getId())
                      .name(tenantConfig.getName())
                      .userProvider(provider)
                      .build();

              tenants.put(tenant.getId(), tenant);
              log.info(
                  "Loaded tenant: {} ({}) with user provider: {}",
                  tenant.getId(),
                  tenant.getName(),
                  provider.getEndpoint());
            });
  }

  @Override
  public Optional<Tenant> findById(String tenantId) {
    return Optional.ofNullable(tenants.get(tenantId));
  }

  @Override
  public List<Tenant> findAll() {
    return new ArrayList<>(tenants.values());
  }
}
