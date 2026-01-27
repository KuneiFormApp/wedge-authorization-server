package com.kuneiform.infrastructure.adapter;

import com.kuneiform.domain.exception.UserProviderException;
import com.kuneiform.domain.model.OAuthClient;
import com.kuneiform.domain.model.Tenant;
import com.kuneiform.domain.model.UserProvider;
import com.kuneiform.domain.port.ClientRepository;
import com.kuneiform.domain.port.MfaRegistrationService;
import com.kuneiform.domain.port.TenantRepository;
import com.kuneiform.infrastructure.adapter.restclients.UserProviderRestClient;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * HTTP-based implementation of MfaRegistrationService.
 *
 * <p>Uses UserProviderRestClient to send MFA registration data to the external user provider.
 * Resolves the MFA endpoint from the UserProvider configuration.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HttpMfaRegistrationAdapter implements MfaRegistrationService {

  private final UserProviderRestClient userProviderRestClient;
  private final ClientRepository clientRepository;
  private final TenantRepository tenantRepository;

  @Override
  public boolean registerMfa(String clientId, String userId, String mfaSecret, String mfaKeyId) {

    log.debug("Registering MFA for user: {} via client: {}", userId, clientId);

    Optional<UserProvider> userProviderOpt = getUserProvider(clientId);
    if (userProviderOpt.isEmpty()) {
      log.error("Cannot register MFA: UserProvider not found for client: {}", clientId);
      return false;
    }

    UserProvider userProvider = userProviderOpt.get();
    String mfaEndpointTemplate = userProvider.getMfaRegistrationEndpoint();

    if (mfaEndpointTemplate == null || mfaEndpointTemplate.isBlank()) {
      log.error(
          "MFA registration endpoint is not configured for client: {} (tenant: {})",
          clientId,
          userProvider);
      return false;
    }

    String mfaEndpoint = mfaEndpointTemplate.replace("{userId}", userId);

    try {
      return userProviderRestClient.registerMfa(mfaEndpoint, mfaSecret, mfaKeyId);
    } catch (Exception e) {
      if (e instanceof UserProviderException userProviderException) {
        log.debug(
            "User provider returned error for MFA registration for user {}: {}",
            userId,
            userProviderException.getMessages());
      } else {
        log.error("Unexpected error during MFA registration for user: {}", userId, e);
      }
      return false;
    }
  }

  /** Resolve user provider for a client by looking up the client's tenant. */
  private Optional<UserProvider> getUserProvider(String clientId) {
    Optional<OAuthClient> clientOpt = clientRepository.findByClientId(clientId);

    if (clientOpt.isEmpty()) {
      log.error("Client not found for clientId: {}", clientId);
      return Optional.empty();
    }

    OAuthClient client = clientOpt.get();
    String tenantId = client.getTenantId();

    if (tenantId == null || tenantId.isBlank()) {
      log.warn("Tenant not configured for client: {}", clientId);
      return Optional.empty();
    }

    Optional<Tenant> tenantOpt = tenantRepository.findById(tenantId);
    if (tenantOpt.isEmpty()) {
      log.error("Tenant not found: {} for client: {}", tenantId, clientId);
      return Optional.empty();
    }

    Tenant tenant = tenantOpt.get();
    if (tenant.getUserProvider() == null) {
      log.warn("User provider not configured for tenant: {}", tenantId);
      return Optional.empty();
    }

    return Optional.of(tenant.getUserProvider());
  }
}
