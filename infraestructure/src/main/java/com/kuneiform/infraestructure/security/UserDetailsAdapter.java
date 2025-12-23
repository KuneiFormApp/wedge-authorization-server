package com.kuneiform.infraestructure.security;

import com.kuneiform.domain.model.User;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Adapter that wraps a domain User and implements Spring Security's UserDetails interface. This
 * allows our domain model to be used with Spring Security's authentication mechanisms.
 */
@RequiredArgsConstructor
public class UserDetailsAdapter implements UserDetails {

  private final User user;

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    List<GrantedAuthority> authorities = new ArrayList<>();

    // Extract authorities from user metadata
    // Note: Authorities should be provided exactly as needed by the user provider
    // This includes roles (ROLE_USER, ROLE_ADMIN), permissions (read:docs,
    // write:users),
    // or any custom authority format
    if (user.getMetadata() != null && user.getMetadata().containsKey("authorities")) {
      Object authoritiesObj = user.getMetadata().get("authorities");

      if (authoritiesObj instanceof Iterable<?>) {
        for (Object authority : (Iterable<?>) authoritiesObj) {
          authorities.add(new SimpleGrantedAuthority(authority.toString()));
        }
      }
    }

    return authorities;
  }

  @Override
  public String getPassword() {
    // WedgeAuth is headless and doesn't store passwords
    // Password validation is done by the HTTP-based user provider
    return "";
  }

  @Override
  public String getUsername() {
    return user.getUsername();
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  /** Get the underlying domain User object. */
  public User getUser() {
    return user;
  }
}
