package com.kuneiform.infrastructure.adapter;

import com.kuneiform.domain.model.UserConsent;
import com.kuneiform.domain.port.ClientRepository;
import com.kuneiform.domain.port.ConsentStoragePort;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * In-memory implementation of ConsentStoragePort. Stores consents in a ConcurrentHashMap for
 * thread-safe access. Suitable for development and single-instance deployments.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "wedge.consent.storage-type",
    havingValue = "in-memory",
    matchIfMissing = true)
public class InMemoryConsentStorageAdapter implements ConsentStoragePort {

  private final ClientRepository clientRepository;

  // Map of "userId:clientId" -> List of granted scopes
  private final Map<String, List<String>> consentStore = new ConcurrentHashMap<>();

  @Override
  public List<UserConsent> findByUserId(String userId) {
    log.debug("Finding consents for user: {}", userId);

    return consentStore.entrySet().stream()
        .filter(entry -> entry.getKey().startsWith(userId + ":"))
        .map(
            entry -> {
              String clientId = entry.getKey().substring((userId + ":").length());
              List<String> grantedScopes = entry.getValue();

              // Fetch client metadata
              var client = clientRepository.findByClientId(clientId);

              return UserConsent.builder()
                  .userId(userId)
                  .clientId(clientId)
                  .grantedScopes(grantedScopes)
                  .clientName(client.map(c -> c.getClientName()).orElse(clientId))
                  .imageUrl(client.map(c -> c.getImageUrl()).orElse(null))
                  .accessUrl(client.map(c -> c.getAccessUrl()).orElse(null))
                  .build();
            })
        .collect(Collectors.toList());
  }

  @Override
  public void revokeByClientId(String userId, String clientId) {
    String key = userId + ":" + clientId;
    consentStore.remove(key);
    log.info("Revoked consent for user: {} and client: {}", userId, clientId);
  }

  @Override
  public boolean hasConsent(String userId, String clientId) {
    String key = userId + ":" + clientId;
    return consentStore.containsKey(key);
  }

  /**
   * Save a consent (for internal use, typically called by Spring OAuth2).
   *
   * @param userId User ID
   * @param clientId Client ID
   * @param scopes List of granted scopes
   */
  public void saveConsent(String userId, String clientId, List<String> scopes) {
    String key = userId + ":" + clientId;
    consentStore.put(key, new ArrayList<>(scopes));
    log.debug(
        "Saved consent for user: {} and client: {} with scopes: {}", userId, clientId, scopes);
  }
}
