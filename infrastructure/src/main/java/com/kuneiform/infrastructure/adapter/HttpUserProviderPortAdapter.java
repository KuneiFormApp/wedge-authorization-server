package com.kuneiform.infrastructure.adapter;

import com.kuneiform.domain.exception.UserProviderException;
import com.kuneiform.domain.model.MfaData;
import com.kuneiform.domain.model.OAuthClient;
import com.kuneiform.domain.model.Tenant;
import com.kuneiform.domain.model.User;
import com.kuneiform.domain.model.UserProvider;
import com.kuneiform.domain.port.ClientRepository;
import com.kuneiform.domain.port.TenantRepository;
import com.kuneiform.domain.port.UserProviderPort;
import com.kuneiform.infrastructure.adapter.models.MfaDataResponse;
import com.kuneiform.infrastructure.adapter.models.UserResponse;
import com.kuneiform.infrastructure.adapter.restclients.UserProviderRestClient;

import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** HTTP-based implementation of UserProvider. Uses UserProviderRestClient for HTTP calls. */
@Slf4j
@Component
@RequiredArgsConstructor
public class HttpUserProviderPortAdapter implements UserProviderPort {

  private final UserProviderRestClient userProviderRestClient;
  private final ClientRepository clientRepository;
  private final TenantRepository tenantRepository;

  @Override
  public Optional<User> findByUsername(String clientId, String tenantId, String username) {
    log.debug(
        "Finding user by username: {} for client: {} (tenant: {})", username, clientId, tenantId);

    Optional<UserProvider> userProviderOpt = getUserProvider(clientId, tenantId);
    if (userProviderOpt.isEmpty()) {
      return Optional.empty();
    }

    UserProvider userProvider = userProviderOpt.get();

    try {
      return userProviderRestClient
          .findByUsername(userProvider.getEndpoint(), username)
          .map(this::mapToUser);
    } catch (Exception e) {
      if (e instanceof UserProviderException userProviderException) {
        log.debug(
            "User provider returned error {}: {}", username, userProviderException.getMessages());
      } else {
        log.error("Unexpected error finding user by username: {}", username, e);
      }
      return Optional.empty();
    }
  }

  @Override
  public Optional<User> validateCredentials(
      String clientId, String tenantId, String username, String password, Set<String> scopes) {
    log.debug(
        "Validating credentials for user: {} for client: {} (tenant: {}) with scopes: {}",
        username,
        clientId,
        tenantId,
        scopes);

    Optional<UserProvider> userProviderOpt = getUserProvider(clientId, tenantId);
    if (userProviderOpt.isEmpty()) {
      return Optional.empty();
    }

    UserProvider userProvider = userProviderOpt.get();

    try {
      return userProviderRestClient
          .validateCredentials(userProvider.getEndpoint(), username, password, scopes)
          .map(this::mapToUser);
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  @Override
  public boolean validateScopes(
      String clientId, String tenantId, String userId, Set<String> scopes) {
    log.debug(
        "Validating scopes for user: {} for client: {} (tenant: {}) scopes: {}",
        userId,
        clientId,
        tenantId,
        scopes);

    if (scopes == null || scopes.isEmpty()) {
      return true;
    }

    Optional<UserProvider> userProviderOpt = getUserProvider(clientId, tenantId);
    if (userProviderOpt.isEmpty()) {
      return false;
    }

    UserProvider userProvider = userProviderOpt.get();

    if (userProvider.getScopesValidationEndpoint() == null) {
      // If no validation endpoint configured, assume all scopes are allowed
      return true;
    }

    try {
      String endpoint = userProvider.getScopesValidationEndpoint().replace("{userId}", userId);
      return userProviderRestClient.validateScopes(endpoint, scopes);
    } catch (Exception e) {
      log.error("Error validating scopes", e);
      return false;
    }
  }

  /**
   * Resolve user provider. If tenantId is provided, look up tenant directly. Otherwise, look up
   * client to find tenant.
   */
  private Optional<UserProvider> getUserProvider(String clientId, String tenantId) {
    // 1. If tenantId is provided, use it directly
    if (tenantId != null && !tenantId.isBlank()) {
      Optional<Tenant> tenantOpt = tenantRepository.findById(tenantId);
      if (tenantOpt.isEmpty()) {
        log.error("Tenant not found: {} (requested with client: {})", tenantId, clientId);
        return Optional.empty();
      }

      Tenant tenant = tenantOpt.get();
      if (tenant.getUserProvider() == null) {
        log.warn("User provider not configured for tenant: {}", tenantId);
        return Optional.empty();
      }

      return Optional.of(tenant.getUserProvider());
    }

    // 2. Fallback: resolve via clientId
    if (clientId == null || clientId.isBlank()) {
      log.warn("Cannot resolve user provider: both tenantId and clientId are missing");
      return Optional.empty();
    }

    Optional<OAuthClient> clientOpt = clientRepository.findByClientId(clientId);

    if (clientOpt.isEmpty()) {
      log.error("Client not found for clientId: {}", clientId);
      return Optional.empty();
    }

    OAuthClient client = clientOpt.get();
    String resolvedTenantId = client.getTenantId();

    if (resolvedTenantId == null || resolvedTenantId.isBlank()) {
      log.warn("Tenant not configured for client: {}", clientId);
      return Optional.empty();
    }

    Optional<Tenant> tenantOpt = tenantRepository.findById(resolvedTenantId);
    if (tenantOpt.isEmpty()) {
      log.error("Tenant not found: {} for client: {}", resolvedTenantId, clientId);
      return Optional.empty();
    }

    Tenant tenant = tenantOpt.get();
    if (tenant.getUserProvider() == null) {
      log.warn("User provider not configured for tenant: {}", resolvedTenantId);
      return Optional.empty();
    }

    return Optional.of(tenant.getUserProvider());
  }

  @Override
  public boolean registerMfa(
      String clientId, String tenantId, String userId, String mfaSecret, String mfaKeyId) {
    log.debug(
        "Registering MFA for user: {} for client: {} (tenant: {})", userId, clientId, tenantId);

    Optional<UserProvider> userProviderOpt = getUserProvider(clientId, tenantId);
    if (userProviderOpt.isEmpty()) {
      return false;
    }

    UserProvider userProvider = userProviderOpt.get();

    if (userProvider.getMfaRegistrationEndpoint() == null) {
      log.warn("MFA registration endpoint not configured for user provider");
      return false;
    }

    try {
      String mfaEndpoint = userProvider.getMfaRegistrationEndpoint().replace("{userId}", userId);
      return userProviderRestClient.registerMfa(mfaEndpoint, mfaSecret, mfaKeyId);
    } catch (Exception e) {
      return false;
    }
  }

  private User mapToUser(UserResponse response) {
    if (response.userId() == null || response.userId().isBlank()) {
      throw new UserProviderException(
          Collections.singletonList("INVALID_RESPONSE"),
          Collections.singletonList("User ID is missing in the response from user provider"),
          Instant.now());
    }
    // Map MFA data if present
    MfaData mfaData = null;
    if (response.mfaData() != null) {
      MfaDataResponse mfaDataResponse = response.mfaData();
      mfaData =
          MfaData.builder()
              .twoFaRegistered(mfaDataResponse.twoFaRegistered())
              .mfaKeyId(mfaDataResponse.mfaKeyId())
              .mfaSecret(mfaDataResponse.mfaSecret())
              .build();
    }

    return User.builder()
        .userId(response.userId())
        .username(response.username())
        .email(response.email())
        .metadata(response.metadata())
        .mfaEnabled(response.mfaEnabled())
        .mfaData(mfaData)
        .build();
  }
}
