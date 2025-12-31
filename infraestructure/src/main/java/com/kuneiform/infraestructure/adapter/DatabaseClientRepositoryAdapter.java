package com.kuneiform.infraestructure.adapter;

import com.kuneiform.domain.model.OAuthClient;
import com.kuneiform.domain.model.UserProviderConfig;
import com.kuneiform.domain.port.ClientRepository;
import com.kuneiform.infraestructure.persistence.entity.OAuthClientEntity;
import com.kuneiform.infraestructure.persistence.repository.OAuthClientJdbcRepository;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Database-backed implementation of ClientRepository using Spring Data JDBC. Includes an
 * in-memory cache for improved performance.
 */
@Slf4j
public class DatabaseClientRepositoryAdapter implements ClientRepository {

  private final OAuthClientJdbcRepository repository;
  private final PasswordEncoder passwordEncoder;
  private final Map<String, OAuthClient> cache = new ConcurrentHashMap<>();

  public DatabaseClientRepositoryAdapter(
      OAuthClientJdbcRepository repository, PasswordEncoder passwordEncoder) {
    this.repository = repository;
    this.passwordEncoder = passwordEncoder;
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
    if (cachedClient != null) {
      return Optional.of(cachedClient);
    }

    // Fall back to database if not in cache
    return repository.findByClientId(clientId).map(this::toOAuthClient);
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
        .userProviderConfig(mapUserProviderConfig(entity))
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

    // User provider config
    if (client.getUserProviderConfig() != null) {
      entity.setUserProviderEnabled(client.getUserProviderConfig().isEnabled());
      entity.setUserProviderEndpoint(client.getUserProviderConfig().getEndpoint());
      entity.setUserProviderTimeout(client.getUserProviderConfig().getTimeout());
    } else {
      entity.setUserProviderEnabled(false);
    }

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

  /** Maps entity user provider fields to UserProviderConfig. */
  private UserProviderConfig mapUserProviderConfig(OAuthClientEntity entity) {
    if (entity.getUserProviderEnabled() == null || !entity.getUserProviderEnabled()) {
      return null;
    }
    return UserProviderConfig.builder()
        .enabled(true)
        .endpoint(entity.getUserProviderEndpoint())
        .timeout(entity.getUserProviderTimeout() != null ? entity.getUserProviderTimeout() : 5000)
        .build();
  }
}
