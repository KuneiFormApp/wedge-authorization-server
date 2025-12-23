package com.kuneiform.infraestructure.security;

import com.kuneiform.application.usecase.AuthenticateUserUseCase;
import com.kuneiform.domain.model.User;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

// Custom authentication provider that uses the HTTP-based user provider to authenticate users
@Slf4j
@Component
@RequiredArgsConstructor
public class HttpUserAuthenticationProvider implements AuthenticationProvider {

  private final AuthenticateUserUseCase authenticateUserUseCase;

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    String username = authentication.getName();
    String password = authentication.getCredentials().toString();

    log.debug("Authenticating user via HTTP provider: {}", username);

    Optional<User> userOpt = authenticateUserUseCase.execute(username, password);

    if (userOpt.isEmpty()) {
      log.warn("Authentication failed for user: {}", username);
      throw new BadCredentialsException("Invalid username or password");
    }

    User user = userOpt.get();
    // The original instruction had a redundant call here:
    // User authenticatedUser = authenticateUserUseCase.execute(username, password);
    // This line is removed to avoid re-executing the use case and to correctly use
    // the 'user' object.

    log.info("User authenticated successfully: {}", username);

    Collection<? extends GrantedAuthority> authorities = extractAuthorities(user);

    return new UsernamePasswordAuthenticationToken(username, password, authorities);
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
  }

  private Collection<? extends GrantedAuthority> extractAuthorities(User user) {
    List<GrantedAuthority> authorities = new ArrayList<>();

    // Extract roles from user metadata and add ROLE_ prefix if missing
    if (user.getMetadata() != null && user.getMetadata().containsKey("roles")) {
      Object rolesObj = user.getMetadata().get("roles");

      if (rolesObj instanceof Iterable<?>) {
        for (Object role : (Iterable<?>) rolesObj) {
          String roleStr = role.toString();
          // Add ROLE_ prefix if not already present
          if (!roleStr.startsWith("ROLE_")) {
            roleStr = "ROLE_" + roleStr;
          }
          authorities.add(new SimpleGrantedAuthority(roleStr));
        }
      }
    }

    // If no roles found, assign default ROLE_USER
    if (authorities.isEmpty()) {
      authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
    }

    return authorities;
  }
}
