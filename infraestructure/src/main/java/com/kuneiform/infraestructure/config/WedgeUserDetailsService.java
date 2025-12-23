package com.kuneiform.infraestructure.config;

import com.kuneiform.domain.model.User;
import com.kuneiform.domain.port.UserProvider;
import com.kuneiform.infraestructure.security.UserDetailsAdapter;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Custom UserDetailsService that loads user details via the HTTP-based user provider. This service
 * is used by Spring Security to load user information during authentication. Note: Password
 * validation is handled by HttpUserAuthenticationProvider, not by this service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WedgeUserDetailsService implements UserDetailsService {

  private final UserProvider userProvider;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    log.debug("Loading user by username: {}", username);

    Optional<User> userOpt = userProvider.findByUsername(username);

    if (userOpt.isEmpty()) {
      log.warn("User not found: {}", username);
      throw new UsernameNotFoundException("User not found: " + username);
    }

    User user = userOpt.get();
    log.debug("User loaded successfully: {}", username);

    return new UserDetailsAdapter(user);
  }
}
