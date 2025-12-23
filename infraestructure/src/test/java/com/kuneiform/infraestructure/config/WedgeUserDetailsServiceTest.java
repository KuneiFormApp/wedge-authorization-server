package com.kuneiform.infraestructure.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.kuneiform.domain.model.User;
import com.kuneiform.domain.port.UserProvider;
import com.kuneiform.infraestructure.security.UserDetailsAdapter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class WedgeUserDetailsServiceTest {

  @Mock private UserProvider userProvider;

  private WedgeUserDetailsService userDetailsService;

  @BeforeEach
  void setUp() {
    userDetailsService = new WedgeUserDetailsService(userProvider);
  }

  @Test
  void shouldLoadUserByUsername() {
    // Given
    User user =
        User.builder()
            .userId("user-123")
            .username("alice")
            .email("alice@example.com")
            .metadata(Map.of("authorities", List.of("ROLE_USER", "ROLE_ADMIN")))
            .build();

    when(userProvider.findByUsername("alice")).thenReturn(Optional.of(user));

    // When
    UserDetails userDetails = userDetailsService.loadUserByUsername("alice");

    // Then
    assertNotNull(userDetails);
    assertEquals("alice", userDetails.getUsername());
    assertTrue(userDetails instanceof UserDetailsAdapter);

    UserDetailsAdapter adapter = (UserDetailsAdapter) userDetails;
    assertEquals(user, adapter.getUser());

    // Verify authorities
    List<String> authorities =
        userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
    assertTrue(authorities.contains("ROLE_USER"));
    assertTrue(authorities.contains("ROLE_ADMIN"));

    verify(userProvider).findByUsername("alice");
  }

  @Test
  void shouldThrowExceptionWhenUserNotFound() {
    // Given
    when(userProvider.findByUsername("nonexistent")).thenReturn(Optional.empty());

    // When & Then
    UsernameNotFoundException exception =
        assertThrows(
            UsernameNotFoundException.class,
            () -> userDetailsService.loadUserByUsername("nonexistent"));

    assertEquals("User not found: nonexistent", exception.getMessage());
    verify(userProvider).findByUsername("nonexistent");
  }

  @Test
  void shouldHandleUserWithoutMetadata() {
    // Given
    User user =
        User.builder()
            .userId("user-456")
            .username("bob")
            .email("bob@example.com")
            .metadata(null)
            .build();

    when(userProvider.findByUsername("bob")).thenReturn(Optional.of(user));

    // When
    UserDetails userDetails = userDetailsService.loadUserByUsername("bob");

    // Then
    assertNotNull(userDetails);
    assertEquals("bob", userDetails.getUsername());

    // Should have no authorities when metadata is null
    List<String> authorities =
        userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
    assertEquals(0, authorities.size());
  }

  @Test
  void shouldHandleUserWithEmptyRoles() {
    // Given
    User user =
        User.builder()
            .userId("user-789")
            .username("charlie")
            .email("charlie@example.com")
            .metadata(Map.of("department", "Engineering"))
            .build();

    when(userProvider.findByUsername("charlie")).thenReturn(Optional.of(user));

    // When
    UserDetails userDetails = userDetailsService.loadUserByUsername("charlie");

    // Then
    assertNotNull(userDetails);
    assertEquals("charlie", userDetails.getUsername());

    // Should have no authorities when 'authorities' key is missing from metadata
    List<String> authorities =
        userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
    assertEquals(0, authorities.size());
  }

  @Test
  void shouldHandleUserWithNullUsername() {
    // Given
    when(userProvider.findByUsername(null)).thenReturn(Optional.empty());

    // When & Then
    assertThrows(
        UsernameNotFoundException.class,
        () -> userDetailsService.loadUserByUsername(null),
        "Should throw UsernameNotFoundException when username is null");
  }

  @Test
  void shouldHandleRolesWithRolePrefix() {
    // Given - User provider sends authorities exactly as they should appear
    User user =
        User.builder()
            .userId("user-101")
            .username("dana")
            .email("dana@example.com")
            .metadata(Map.of("authorities", List.of("ROLE_ADMIN", "SUPER_USER")))
            .build();

    when(userProvider.findByUsername("dana")).thenReturn(Optional.of(user));

    // When
    UserDetails userDetails = userDetailsService.loadUserByUsername("dana");

    // Then
    List<String> authorities =
        userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();

    assertTrue(authorities.contains("ROLE_ADMIN"));
    assertTrue(authorities.contains("SUPER_USER")); // No auto-prefix, used as-is
    assertEquals(2, authorities.size());
  }
}
