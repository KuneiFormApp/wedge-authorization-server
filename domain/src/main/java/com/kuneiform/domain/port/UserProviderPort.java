package com.kuneiform.domain.port;

import com.kuneiform.domain.model.User;
import java.util.Optional;

public interface UserProviderPort {
  Optional<User> findByUsername(String clientId, String tenantId, String username);

  Optional<User> validateCredentials(
      String clientId, String tenantId, String username, String password);
}
