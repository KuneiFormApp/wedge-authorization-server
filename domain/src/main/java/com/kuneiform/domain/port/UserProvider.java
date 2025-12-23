package com.kuneiform.domain.port;

import com.kuneiform.domain.model.User;
import java.util.Optional;

public interface UserProvider {
  Optional<User> findByUsername(String username);

  Optional<User> validateCredentials(String username, String password);
}
