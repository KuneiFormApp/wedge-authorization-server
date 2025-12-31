package com.kuneiform.infraestructure.adapter;

import com.kuneiform.domain.model.OAuthClient;
import com.kuneiform.domain.model.UserProviderConfig;
import com.kuneiform.domain.port.ClientRepository;
import com.kuneiform.infraestructure.config.properties.WedgeConfigProperties;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;

/** YAML-based implementation of ClientRepository that loads clients from application.yaml. */
@Slf4j
public class YamlClientRepositoryAdapter implements ClientRepository {

  private final Map<String, OAuthClient> clients = new ConcurrentHashMap<>();
  private final PasswordEncoder passwordEncoder;

  public YamlClientRepositoryAdapter(
      WedgeConfigProperties config, PasswordEncoder passwordEncoder) {
    this.passwordEncoder = passwordEncoder;
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
                      .userProviderConfig(mapUserProviderConfig(clientConfig.getUserProvider()))
                      .build();

              clients.put(client.getClientId(), client);
              log.info(
                  "Loaded OAuth client: {} (public={})", client.getClientId(), client.isPublic());
            });
  }

  private <T> HashSet<T> toSetOrEmpty(List<T> list) {
    return list == null ? new HashSet<>() : new HashSet<>(list);
  }

  private String encodeSecretIfPresent(String secret) {
    if (secret == null || secret.isBlank()) {
      return null;
    }
    return passwordEncoder.encode(secret);
  }

  private UserProviderConfig mapUserProviderConfig(
      WedgeConfigProperties.UserProviderConfig config) {
    if (config == null) {
      return null;
    }
    return UserProviderConfig.builder()
        .enabled(config.isEnabled())
        .endpoint(config.getEndpoint())
        .timeout(config.getTimeout())
        .build();
  }

  @Override
  public Optional<OAuthClient> findByClientId(String clientId) {
    return Optional.ofNullable(clients.get(clientId));
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
