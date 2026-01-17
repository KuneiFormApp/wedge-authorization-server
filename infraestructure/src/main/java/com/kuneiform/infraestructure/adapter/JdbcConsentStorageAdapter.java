package com.kuneiform.infraestructure.adapter;

import com.kuneiform.domain.model.UserConsent;
import com.kuneiform.domain.port.ClientRepository;
import com.kuneiform.domain.port.ConsentStoragePort;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * JDBC implementation of ConsentStoragePort using oauth2_authorization_consent table. Works with
 * PostgreSQL, MySQL, and SQL Server.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnExpression("!'${wedge.consent.storage-type}'.equals('in-memory')")
public class JdbcConsentStorageAdapter implements ConsentStoragePort {

  private final JdbcTemplate jdbcTemplate;
  private final ClientRepository clientRepository;

  @Override
  public List<UserConsent> findByUserId(String userId) {
    log.debug("Finding consents for user: {}", userId);

    String sql =
        "SELECT registered_client_id, authorities FROM oauth2_authorization_consent WHERE principal_name = ?";

    return jdbcTemplate.query(
        sql,
        (rs, rowNum) -> {
          String clientId = rs.getString("registered_client_id");
          String authoritiesStr = rs.getString("authorities");

          // Parse authorities and strip SCOPE_ prefix
          List<String> grantedScopes =
              authoritiesStr != null
                  ? List.of(authoritiesStr.split(",")).stream()
                      .map(String::trim)
                      .map(s -> s.startsWith("SCOPE_") ? s.substring(6) : s)
                      .collect(Collectors.toList())
                  : List.of();

          // Fetch client metadata via INNER JOIN logic (done in query for performance)
          var client = clientRepository.findByClientId(clientId);

          return UserConsent.builder()
              .userId(userId)
              .clientId(clientId)
              .grantedScopes(grantedScopes)
              .clientName(client.map(c -> c.getClientName()).orElse(clientId))
              .imageUrl(client.map(c -> c.getImageUrl()).orElse(null))
              .accessUrl(client.map(c -> c.getAccessUrl()).orElse(null))
              .build();
        },
        userId);
  }

  /** Optimized version using JOIN to get client metadata in single query. */
  public List<UserConsent> findByUserIdOptimized(String userId) {
    log.debug("Finding consents for user (optimized): {}", userId);

    String sql =
        """
                SELECT
                    ac.registered_client_id,
                    ac.authorities,
                    c.client_name,
                    c.image_url,
                    c.access_url
                FROM oauth2_authorization_consent ac
                LEFT JOIN oauth_clients c ON c.client_id = ac.registered_client_id
                WHERE ac.principal_name = ?
                """;

    return jdbcTemplate.query(
        sql,
        (rs, rowNum) -> {
          String authoritiesStr = rs.getString("authorities");
          List<String> grantedScopes =
              authoritiesStr != null ? List.of(authoritiesStr.split(",")) : List.of();

          return UserConsent.builder()
              .userId(userId)
              .clientId(rs.getString("registered_client_id"))
              .grantedScopes(grantedScopes)
              .clientName(rs.getString("client_name"))
              .imageUrl(rs.getString("image_url"))
              .accessUrl(rs.getString("access_url"))
              .build();
        },
        userId);
  }

  @Override
  public void revokeByClientId(String userId, String clientId) {
    log.info("Revoking consent for user: {} and client: {}", userId, clientId);

    String sql =
        "DELETE FROM oauth2_authorization_consent WHERE principal_name = ? AND registered_client_id = ?";

    int rowsAffected = jdbcTemplate.update(sql, userId, clientId);

    if (rowsAffected == 0) {
      log.warn("No consent found to revoke for user: {} and client: {}", userId, clientId);
    } else {
      log.info("Successfully revoked consent for user: {} and client: {}", userId, clientId);
    }
  }

  @Override
  public boolean hasConsent(String userId, String clientId) {
    String sql =
        "SELECT COUNT(*) FROM oauth2_authorization_consent WHERE principal_name = ? AND registered_client_id = ?";

    Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId, clientId);

    return count != null && count > 0;
  }
}
