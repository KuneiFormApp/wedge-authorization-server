package com.kuneiform.infraestructure.adapter;

import com.kuneiform.domain.port.TenantClientPort;
import com.kuneiform.infraestructure.config.properties.WedgeConfigProperties;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * In-memory implementation of TenantClientPort for YAML configuration mode. Loads tenant-client
 * relationships from YAML and stores in memory.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "wedge.client-storage.type",
    havingValue = "none",
    matchIfMissing = true)
public class InMemoryTenantClientAdapter implements TenantClientPort {

  private final WedgeConfigProperties wedgeConfig;

  // Map of tenantId -> Set of clientIds
  private final Map<String, List<String>> tenantClients = new ConcurrentHashMap<>();

  /**
   * Initialize tenant-client relationships from YAML configuration. Maps each client to its
   * configured tenant.
   */
  @jakarta.annotation.PostConstruct
  public void init() {
    log.info("Initializing in-memory tenant-client relationships from YAML");

    // Clear existing mappings
    tenantClients.clear();

    // Load from clients configuration
    wedgeConfig
        .getClients()
        .forEach(
            clientConfig -> {
              String tenantId = clientConfig.getTenantId();
              String clientId = clientConfig.getClientId();

              tenantClients
                  .computeIfAbsent(tenantId, k -> new java.util.ArrayList<>())
                  .add(clientId);
              log.debug("Mapped client '{}' to tenant '{}'", clientId, tenantId);
            });

    log.info("Loaded {} tenant(s) with client mappings", tenantClients.size());
  }

  @Override
  public List<String> getClientIdsForTenant(String tenantId) {
    return tenantClients.getOrDefault(tenantId, List.of());
  }

  @Override
  public boolean isClientInTenant(String tenantId, String clientId) {
    return tenantClients.getOrDefault(tenantId, List.of()).contains(clientId);
  }

  @Override
  public void addClientToTenant(String tenantId, String clientId) {
    tenantClients.computeIfAbsent(tenantId, k -> new java.util.ArrayList<>()).add(clientId);
    log.info("Added client '{}' to tenant '{}'", clientId, tenantId);
  }

  @Override
  public void removeClientFromTenant(String tenantId, String clientId) {
    List<String> clients = tenantClients.get(tenantId);
    if (clients != null) {
      clients.remove(clientId);
      log.info("Removed client '{}' from tenant '{}'", clientId, tenantId);
    }
  }
}
