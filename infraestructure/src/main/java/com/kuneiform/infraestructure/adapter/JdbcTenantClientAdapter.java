package com.kuneiform.infraestructure.adapter;

import com.kuneiform.domain.port.TenantClientPort;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * JDBC implementation of TenantClientPort using oauth_clients.tenant_id. Queries the existing
 * tenant_id column on oauth_clients table instead of a junction table. Works with PostgreSQL,
 * MySQL, and SQL Server.
 */
@Slf4j
@Component
@ConditionalOnExpression(
    "!'${wedge.client-storage.type:none}'.equals('none') && !'${wedge.client-storage.type}'.equals('in-memory')")
public class JdbcTenantClientAdapter implements TenantClientPort {

  private final JdbcTemplate jdbcTemplate;

  public JdbcTenantClientAdapter(@Qualifier("clientJdbcTemplate") JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public List<String> getClientIdsForTenant(String tenantId) {
    String sql = "SELECT client_id FROM oauth_clients WHERE tenant_id = ?";
    return jdbcTemplate.queryForList(sql, String.class, tenantId);
  }

  @Override
  public boolean isClientInTenant(String tenantId, String clientId) {
    String sql = "SELECT COUNT(*) FROM oauth_clients WHERE tenant_id = ? AND client_id = ?";
    Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tenantId, clientId);
    return count != null && count > 0;
  }

  @Override
  public void addClientToTenant(String tenantId, String clientId) {
    // Update the client's tenant_id instead of inserting into junction table
    String sql = "UPDATE oauth_clients SET tenant_id = ? WHERE client_id = ?";
    int rows = jdbcTemplate.update(sql, tenantId, clientId);

    if (rows > 0) {
      log.info("Set tenant '{}' for client '{}'", tenantId, clientId);
    } else {
      log.warn("Client '{}' not found, cannot set tenant", clientId);
    }
  }

  @Override
  public void removeClientFromTenant(String tenantId, String clientId) {
    // Clear the tenant_id for this client
    String sql = "UPDATE oauth_clients SET tenant_id = NULL WHERE tenant_id = ? AND client_id = ?";
    int rows = jdbcTemplate.update(sql, tenantId, clientId);

    if (rows > 0) {
      log.info("Removed tenant '{}' from client '{}'", tenantId, clientId);
    } else {
      log.warn("No client '{}' found with tenant '{}'", clientId, tenantId);
    }
  }
}
