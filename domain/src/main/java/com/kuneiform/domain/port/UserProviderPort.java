package com.kuneiform.domain.port;

import com.kuneiform.domain.model.User;
import java.util.Optional;
import java.util.Set;

public interface UserProviderPort {
  Optional<User> findByUsername(String clientId, String tenantId, String username);

  Optional<User> validateCredentials(
      String clientId, String tenantId, String username, String password, Set<String> scopes);

  boolean validateScopes(String clientId, String tenantId, String userId, Set<String> scopes);

  boolean registerMfa(
      String clientId, String tenantId, String userId, String mfaSecret, String mfaKeyId);
}
