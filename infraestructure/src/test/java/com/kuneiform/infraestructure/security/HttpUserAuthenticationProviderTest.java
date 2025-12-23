package com.kuneiform.infraestructure.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.kuneiform.application.usecase.AuthenticateUserUseCase;
import com.kuneiform.domain.model.User;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

@ExtendWith(MockitoExtension.class)
class HttpUserAuthenticationProviderTest {

  @Mock private AuthenticateUserUseCase authenticateUserUseCase;

  private HttpUserAuthenticationProvider provider;

  @BeforeEach
  void setUp() {
    provider = new HttpUserAuthenticationProvider(authenticateUserUseCase);
  }

  @Test
  void shouldAuthenticateValidUser() {
    User user =
        User.builder()
            .userId("user-123")
            .username("alice")
            .email("alice@example.com")
            .metadata(Map.of("roles", List.of("USER", "ADMIN")))
            .build();

    when(authenticateUserUseCase.execute("alice", "password123")).thenReturn(Optional.of(user));

    Authentication auth = new UsernamePasswordAuthenticationToken("alice", "password123");
    Authentication result = provider.authenticate(auth);

    assertNotNull(result);
    assertTrue(result.isAuthenticated());
    assertEquals("alice", result.getName());
    assertEquals("alice", result.getPrincipal());

    // Should have ROLE_USER, ROLE_USER (from metadata), and ROLE_ADMIN
    List<String> authorities =
        result.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
    assertTrue(authorities.contains("ROLE_USER"));
    assertTrue(authorities.contains("ROLE_ADMIN"));
  }

  @Test
  void shouldRejectInvalidCredentials() {
    when(authenticateUserUseCase.execute("alice", "wrongpassword")).thenReturn(Optional.empty());

    Authentication auth = new UsernamePasswordAuthenticationToken("alice", "wrongpassword");

    assertThrows(BadCredentialsException.class, () -> provider.authenticate(auth));
  }

  @Test
  void shouldSupportUsernamePasswordAuthentication() {
    assertTrue(provider.supports(UsernamePasswordAuthenticationToken.class));
  }

  @Test
  void shouldAddRolePrefixToRolesWithoutIt() {
    User user =
        User.builder()
            .userId("user-123")
            .username("alice")
            .metadata(Map.of("roles", List.of("USER", "ADMIN")))
            .build();

    when(authenticateUserUseCase.execute("alice", "password")).thenReturn(Optional.of(user));

    Authentication auth = new UsernamePasswordAuthenticationToken("alice", "password");
    Authentication result = provider.authenticate(auth);

    List<String> authorities =
        result.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();

    // All roles should have ROLE_ prefix
    assertTrue(authorities.stream().allMatch(a -> a.startsWith("ROLE_")));
  }

  @Test
  void shouldHandleUserWithoutRoles() {
    User user =
        User.builder()
            .userId("user-123")
            .username("alice")
            .metadata(Map.of("department", "Engineering"))
            .build();

    when(authenticateUserUseCase.execute("alice", "password")).thenReturn(Optional.of(user));

    Authentication auth = new UsernamePasswordAuthenticationToken("alice", "password");
    Authentication result = provider.authenticate(auth);

    // Should still have default ROLE_USER
    List<String> authorities =
        result.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
    assertEquals(1, authorities.size());
    assertTrue(authorities.contains("ROLE_USER"));
  }
}
