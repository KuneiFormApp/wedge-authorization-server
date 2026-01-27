package com.kuneiform.infrastructure.adapter;

import com.kuneiform.domain.model.OAuthClient;
import com.kuneiform.domain.port.ClientRepository;
import com.kuneiform.domain.port.TenantClientPort;
import com.kuneiform.infrastructure.config.properties.WedgeConfigProperties;
import com.kuneiform.infrastructure.security.TenantContext;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * YAML-based implementation of ClientRepository that loads clients from application.yaml. Supports
 * multi-tenancy by filtering clients based on TenantContext.
 */
@Slf4j
public class YamlClientRepositoryAdapter implements ClientRepository {

  private final Map<String, OAuthClient> clients = new ConcurrentHashMap<>();
  private final PasswordEncoder passwordEncoder;
  private final TenantClientPort tenantClientPort;

  public YamlClientRepositoryAdapter(
      WedgeConfigProperties config,
      PasswordEncoder passwordEncoder,
      TenantClientPort tenantClientPort) {
    this.passwordEncoder = passwordEncoder;
    this.tenantClientPort = tenantClientPort;
    loadClients(config);
  }

  private void loadClients(WedgeConfigProperties config) {
    config
        .getClients()
        .forEach(
            clientConfig -> {
              OAuthClient client =
                  OAuthClient.builder()
                      .clientId(clientConfig.getClientId())
                      .clientSecret(encodeSecretIfPresent(clientConfig.getClientSecret()))
                      .clientName(clientConfig.getClientName())
                      .clientAuthenticationMethods(
                          toSetOrEmpty(clientConfig.getClientAuthenticationMethods()))
                      .authorizationGrantTypes(
                          toSetOrEmpty(clientConfig.getAuthorizationGrantTypes()))
                      .redirectUris(toSetOrEmpty(clientConfig.getRedirectUris()))
                      .postLogoutRedirectUris(
                          toSetOrEmpty(clientConfig.getPostLogoutRedirectUris()))
                      .scopes(toSetOrEmpty(clientConfig.getScopes()))
                      .requireAuthorizationConsent(clientConfig.isRequireAuthorizationConsent())
                      .requirePkce(clientConfig.isRequirePkce())
                      .tenantId(clientConfig.getTenantId())
                      .imageUrl(clientConfig.getImageUrl())
                      .accessUrl(clientConfig.getAccessUrl())
                      .build();

              clients.put(client.getClientId(), client);
              log.info(
                  "Loaded OAuth client: {} (public={}, tenant={})",
                  client.getClientId(),
                  client.isPublic(),
                  client.getTenantId());
            });
  }

  private <T> HashSet<T> toSetOrEmpty(List<T> list) {
    return list == null ? new HashSet<>() : new HashSet<>(list);
  }

  private String encodeSecretIfPresent(String secret) {
    if (secret == null || secret.isBlank()) {
      return null;
    }
    // Skip encoding if already a bcrypt hash
    if (secret.startsWith("$2a$") || secret.startsWith("$2b$") || secret.startsWith("$2y$")) {
      return secret;
    }
    return passwordEncoder.encode(secret);
  }

  @Override
  public Optional<OAuthClient> findByClientId(String clientId) {
    Optional<OAuthClient> clientOpt = Optional.ofNullable(clients.get(clientId));

    // If no tenant context or client not found, return as-is
    if (!TenantContext.hasTenant() || clientOpt.isEmpty()) {
      return clientOpt;
    }

    // Check if client belongs to current tenant
    String currentTenant = TenantContext.getCurrentTenant();
    if (tenantClientPort.isClientInTenant(currentTenant, clientId)) {
      return clientOpt;
    }

    // Client exists but not in current tenant
    log.debug("Client '{}' not accessible in tenant '{}'", clientId, currentTenant);
    return Optional.empty();
  }

  @Override
  public boolean validateClient(String clientId, String clientSecret) {
    Optional<OAuthClient> clientOpt = findByClientId(clientId);

    if (clientOpt.isEmpty()) {
      return false;
    }

    OAuthClient client = clientOpt.get();

    // Public clients - only validate client ID exists
    if (client.isPublic()) {
      return true;
    }

    // Confidential clients - validate secret
    if (clientSecret == null) {
      return false;
    }

    return passwordEncoder.matches(clientSecret, client.getClientSecret());
  }
}
