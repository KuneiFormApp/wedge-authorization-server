package com.kuneiform.infrastructure.adapter;

import com.kuneiform.domain.model.OAuthClient;
import com.kuneiform.domain.port.ClientRepository;
import com.kuneiform.domain.port.TenantClientPort;
import com.kuneiform.infrastructure.config.properties.WedgeConfigProperties;
import com.kuneiform.infrastructure.persistence.entity.OAuthClientEntity;
import com.kuneiform.infrastructure.persistence.repository.OAuthClientJdbcRepository;
import com.kuneiform.infrastructure.security.TenantContext;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Database-backed implementation of ClientRepository using Spring Data JDBC. Includes an in-memory
 * cache for improved performance. Supports multi-tenancy by filtering clients based on
 * TenantContext.
 */
@Slf4j
public class DatabaseClientRepositoryAdapter implements ClientRepository {

  private final OAuthClientJdbcRepository repository;
  private final PasswordEncoder passwordEncoder;
  private final TenantClientPort tenantClientPort;
  private final boolean multiTenancyEnabled;
  private final Map<String, OAuthClient> cache = new ConcurrentHashMap<>();

  public DatabaseClientRepositoryAdapter(
      OAuthClientJdbcRepository repository,
      PasswordEncoder passwordEncoder,
      TenantClientPort tenantClientPort,
      WedgeConfigProperties config) {
    this.repository = repository;
    this.passwordEncoder = passwordEncoder;
    this.tenantClientPort = tenantClientPort;
    this.multiTenancyEnabled =
        config.getMultiTenancy() != null && config.getMultiTenancy().isEnabled();
    loadCache();
  }

  /** Loads all clients from the database into the cache on startup. */
  private void loadCache() {
    repository
        .findAll()
        .forEach(
            entity -> {
              OAuthClient client = toOAuthClient(entity);
              cache.put(client.getClientId(), client);
              log.info(
                  "Loaded OAuth client from database: {} (public={})",
                  client.getClientId(),
                  client.isPublic());
            });
    log.info("Loaded {} OAuth clients from database into cache", cache.size());
  }

  @Override
  public Optional<OAuthClient> findByClientId(String clientId) {
    // Check cache first
    OAuthClient cachedClient = cache.get(clientId);
    Optional<OAuthClient> clientOpt =
        cachedClient != null
            ? Optional.of(cachedClient)
            : repository.findByClientId(clientId).map(this::toOAuthClient);

    // If multi-tenancy is disabled or client not found, return as-is
    if (!multiTenancyEnabled || clientOpt.isEmpty()) {
      return clientOpt;
    }

    // Multi-tenancy is enabled - check if client belongs to current tenant
    if (!TenantContext.hasTenant()) {
      log.warn("Multi-tenancy enabled but no tenant context set for client lookup: {}", clientId);
      return clientOpt; // Allow for backward compatibility
    }

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

  @Override
  public List<OAuthClient> findAll() {
    return StreamSupport.stream(repository.findAll().spliterator(), false)
        .map(this::toOAuthClient)
        .collect(Collectors.toList());
  }

  @Override
  public OAuthClient save(OAuthClient client) {
    OAuthClientEntity entity = toEntity(client);

    // Encode secret if present and not already encoded
    if (entity.getClientSecret() != null
        && !entity.getClientSecret().isBlank()
        && !entity.getClientSecret().startsWith("$")) {
      entity.setClientSecret(passwordEncoder.encode(entity.getClientSecret()));
    }

    OAuthClientEntity savedEntity = repository.save(entity);
    OAuthClient savedClient = toOAuthClient(savedEntity);

    // Update cache
    cache.put(savedClient.getClientId(), savedClient);
    log.info("Saved OAuth client to database: {}", savedClient.getClientId());

    return savedClient;
  }

  @Override
  public void deleteByClientId(String clientId) {
    repository.deleteByClientId(clientId);
    cache.remove(clientId);
    log.info("Deleted OAuth client from database: {}", clientId);
  }

  /** Converts database entity to domain model. */
  private OAuthClient toOAuthClient(OAuthClientEntity entity) {
    return OAuthClient.builder()
        .id(entity.getId())
        .clientId(entity.getClientId())
        .clientSecret(entity.getClientSecret())
        .clientName(entity.getClientName())
        .clientAuthenticationMethods(parseSet(entity.getClientAuthenticationMethods()))
        .authorizationGrantTypes(parseSet(entity.getAuthorizationGrantTypes()))
        .redirectUris(parseSet(entity.getRedirectUris()))
        .postLogoutRedirectUris(parseSet(entity.getPostLogoutRedirectUris()))
        .scopes(parseSet(entity.getScopes()))
        .requireAuthorizationConsent(
            entity.getRequireAuthorizationConsent() != null
                && entity.getRequireAuthorizationConsent())
        .requirePkce(entity.getRequirePkce() != null && entity.getRequirePkce())
        .tenantId(entity.getTenantId())
        .imageUrl(entity.getImageUrl())
        .accessUrl(entity.getAccessUrl())
        .build();
  }

  /** Converts domain model to database entity. */
  private OAuthClientEntity toEntity(OAuthClient client) {
    OAuthClientEntity entity = new OAuthClientEntity();
    entity.setId(client.getId());
    entity.setClientId(client.getClientId());
    entity.setClientSecret(client.getClientSecret());
    entity.setClientName(client.getClientName());
    entity.setClientAuthenticationMethods(serializeSet(client.getClientAuthenticationMethods()));
    entity.setAuthorizationGrantTypes(serializeSet(client.getAuthorizationGrantTypes()));
    entity.setRedirectUris(serializeSet(client.getRedirectUris()));
    entity.setPostLogoutRedirectUris(serializeSet(client.getPostLogoutRedirectUris()));
    entity.setScopes(serializeSet(client.getScopes()));
    entity.setRequireAuthorizationConsent(client.isRequireAuthorizationConsent());
    entity.setRequirePkce(client.isRequirePkce());
    entity.setTenantId(client.getTenantId());
    entity.setImageUrl(client.getImageUrl());
    entity.setAccessUrl(client.getAccessUrl());

    return entity;
  }

  /** Parses comma-separated string to Set. */
  private Set<String> parseSet(String commaSeparated) {
    if (commaSeparated == null || commaSeparated.isBlank()) {
      return new HashSet<>();
    }
    return Arrays.stream(commaSeparated.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .collect(Collectors.toSet());
  }

  /** Serializes Set to comma-separated string. */
  private String serializeSet(Set<String> set) {
    if (set == null || set.isEmpty()) {
      return "";
    }
    return String.join(",", set);
  }
}
