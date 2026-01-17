package com.kuneiform.infraestructure.adapter;

import com.kuneiform.domain.model.MfaData;
import com.kuneiform.domain.model.OAuthClient;
import com.kuneiform.domain.model.Tenant;
import com.kuneiform.domain.model.User;
import com.kuneiform.domain.model.UserProvider;
import com.kuneiform.domain.port.ClientRepository;
import com.kuneiform.domain.port.TenantRepository;
import com.kuneiform.domain.port.UserProviderPort;
import com.kuneiform.infraestructure.adapter.models.MfaDataResponse;
import com.kuneiform.infraestructure.adapter.models.UserResponse;
import com.kuneiform.infraestructure.adapter.restclients.UserProviderRestClient;
import java.util.Optional;
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

    return userProviderRestClient
        .findByUsername(userProvider.getEndpoint(), username)
        .map(this::mapToUser);
  }

  @Override
  public Optional<User> validateCredentials(
      String clientId, String tenantId, String username, String password) {
    log.debug(
        "Validating credentials for user: {} for client: {} (tenant: {})",
        username,
        clientId,
        tenantId);

    Optional<UserProvider> userProviderOpt = getUserProvider(clientId, tenantId);
    if (userProviderOpt.isEmpty()) {
      return Optional.empty();
    }

    UserProvider userProvider = userProviderOpt.get();

    return userProviderRestClient
        .validateCredentials(userProvider.getEndpoint(), username, password)
        .map(this::mapToUser);
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

  private User mapToUser(UserResponse response) {
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
