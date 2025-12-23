package com.kuneiform.infraestructure.security;

import com.kuneiform.application.usecase.AuthenticateUserUseCase;
import com.kuneiform.domain.model.User;
import com.kuneiform.infraestructure.config.properties.WedgeConfigProperties;
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
  private final WedgeConfigProperties config;

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    String username = authentication.getName();
    String password = authentication.getCredentials().toString();

    log.debug("Authenticating user via HTTP provider: {}", username);

    // TODO: Extract clientId from session/request context when available
    // For now, use the first available client with user-provider enabled
    String clientId = determineClientId();

    if (clientId == null) {
      log.error("No client with user-provider enabled found");
      throw new BadCredentialsException("Authentication service unavailable");
    }

    Optional<User> userOpt = authenticateUserUseCase.execute(clientId, username, password);

    if (userOpt.isEmpty()) {
      log.warn("Authentication failed for user: {}", username);
      throw new BadCredentialsException("Invalid username or password");
    }

    User user = userOpt.get();

    log.info("User authenticated successfully: {}", username);

    Collection<? extends GrantedAuthority> authorities = extractAuthorities(user);

    // Store the User object as the principal so it's available for token
    // customization
    return new UsernamePasswordAuthenticationToken(user, password, authorities);
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
  }

  private String determineClientId() {
    // Find the first client with user-provider enabled
    return config.getClients().stream()
        .filter(client -> client.getUserProvider() != null && client.getUserProvider().isEnabled())
        .map(WedgeConfigProperties.ClientConfig::getClientId)
        .findFirst()
        .orElse(null);
  }

  private Collection<? extends GrantedAuthority> extractAuthorities(User user) {
    List<GrantedAuthority> authorities = new ArrayList<>();

    if (user.getMetadata() != null && user.getMetadata().containsKey("authorities")) {
      Object authoritiesObj = user.getMetadata().get("authorities");

      if (authoritiesObj instanceof Iterable<?>) {
        for (Object authority : (Iterable<?>) authoritiesObj) {
          String authorityStr = authority.toString();
          authorities.add(new SimpleGrantedAuthority(authorityStr));
        }
      }
    }

    if (authorities.isEmpty()) {
      authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
    }

    return authorities;
  }
}
