package com.kuneiform.infraestructure.security;

import static org.junit.jupiter.api.Assertions.*;

import com.kuneiform.domain.model.User;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

/**
 * Tests for UserDetailsAdapter.
 *
 * <p>Important: Since WedgeAuth is headless, authorities should be provided exactly as needed by
 * the user provider. This includes:
 *
 * <ul>
 *   <li>Roles with ROLE_ prefix (e.g., "ROLE_USER", "ROLE_ADMIN")
 *   <li>Permissions (e.g., "read:documents", "write:users")
 *   <li>Custom authorities (e.g., "FEATURE_X", "ACCESS_LEVEL_3")
 * </ul>
 */
class UserDetailsAdapterTest {

  @Test
  void shouldImplementUserDetailsContract() {
    // Given
    User user =
        User.builder()
            .userId("user-123")
            .username("alice")
            .email("alice@example.com")
            .metadata(Map.of("authorities", List.of("ROLE_USER", "ROLE_ADMIN")))
            .build();

    // When
    UserDetailsAdapter adapter = new UserDetailsAdapter(user);

    // Then
    assertEquals("alice", adapter.getUsername());
    assertEquals("", adapter.getPassword()); // Empty for headless auth
    assertTrue(adapter.isAccountNonExpired());
    assertTrue(adapter.isAccountNonLocked());
    assertTrue(adapter.isCredentialsNonExpired());
    assertTrue(adapter.isEnabled());
    assertEquals(user, adapter.getUser());
  }

  @Test
  void shouldExtractAuthoritiesFromMetadata() {
    // Given - User provider sends authorities with proper ROLE_ prefix
    User user =
        User.builder()
            .userId("user-123")
            .username("alice")
            .metadata(Map.of("authorities", List.of("ROLE_USER", "ROLE_ADMIN", "ROLE_MODERATOR")))
            .build();

    // When
    UserDetailsAdapter adapter = new UserDetailsAdapter(user);

    // Then
    List<String> authorities =
        adapter.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();

    assertEquals(3, authorities.size());
    assertTrue(authorities.contains("ROLE_USER"));
    assertTrue(authorities.contains("ROLE_ADMIN"));
    assertTrue(authorities.contains("ROLE_MODERATOR"));
  }

  @Test
  void shouldHandleMixedRolesAndPermissions() {
    // Given - User provider can send mix of roles and permissions
    User user =
        User.builder()
            .userId("user-456")
            .username("bob")
            .email("bob@example.com")
            .metadata(
                Map.of("authorities", List.of("ROLE_SUPER_USER", "read:documents", "write:users")))
            .build();

    // When
    UserDetailsAdapter adapter = new UserDetailsAdapter(user);

    // Then
    List<String> authorities =
        adapter.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();

    assertTrue(authorities.contains("ROLE_SUPER_USER"));
    assertTrue(authorities.contains("read:documents"));
    assertTrue(authorities.contains("write:users"));
    assertEquals(3, authorities.size());
  }

  @Test
  void shouldHandleNullMetadata() {
    // Given
    User user = User.builder().userId("user-123").username("dana").metadata(null).build();

    // When
    UserDetailsAdapter adapter = new UserDetailsAdapter(user);

    // Then
    List<String> authorities =
        adapter.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();

    assertEquals(0, authorities.size()); // No authorities when metadata is null
  }

  @Test
  void shouldHandleEmptyMetadata() {
    // Given
    User user = User.builder().userId("user-123").username("eve").metadata(Map.of()).build();

    // When
    UserDetailsAdapter adapter = new UserDetailsAdapter(user);

    // Then
    List<String> authorities =
        adapter.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();

    assertEquals(
        0, authorities.size()); // No authorities when metadata doesn't have authorities key
  }

  @Test
  void shouldHandleMetadataWithoutAuthorities() {
    // Given
    User user =
        User.builder()
            .userId("user-123")
            .username("frank")
            .metadata(Map.of("department", "Engineering", "level", 5))
            .build();

    // When
    UserDetailsAdapter adapter = new UserDetailsAdapter(user);

    // Then
    List<String> authorities =
        adapter.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();

    assertEquals(0, authorities.size()); // No authorities when 'authorities' key is missing
  }

  @Test
  void shouldHandleEmptyAuthoritiesList() {
    // Given
    User user =
        User.builder()
            .userId("user-123")
            .username("grace")
            .metadata(Map.of("authorities", List.of()))
            .build();

    // When
    UserDetailsAdapter adapter = new UserDetailsAdapter(user);

    // Then
    List<String> authorities =
        adapter.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();

    assertEquals(0, authorities.size()); // No authorities when list is empty
  }

  @Test
  void shouldHandleAuthoritiesAsNonIterableType() {
    // Given - Invalid format: authorities should be a list
    User user =
        User.builder()
            .userId("user-123")
            .username("henry")
            .metadata(Map.of("authorities", "ROLE_ADMIN")) // String instead of List
            .build();

    // When
    UserDetailsAdapter adapter = new UserDetailsAdapter(user);

    // Then
    List<String> authorities =
        adapter.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();

    // Should have no authorities since it's not iterable
    assertEquals(0, authorities.size());
  }

  @Test
  void shouldHandleCustomAuthorityFormats() {
    // Given - User provider can send any authority format
    User user =
        User.builder()
            .userId("user-789")
            .username("iris")
            .metadata(
                Map.of(
                    "authorities",
                    List.of(
                        "ROLE_ADMIN",
                        "read:documents",
                        "write:documents",
                        "FEATURE_BETA",
                        "ACCESS_LEVEL_5")))
            .build();

    // When
    UserDetailsAdapter adapter = new UserDetailsAdapter(user);

    // Then
    List<String> authorities =
        adapter.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();

    assertTrue(authorities.contains("ROLE_ADMIN"));
    assertTrue(authorities.contains("read:documents"));
    assertTrue(authorities.contains("write:documents"));
    assertTrue(authorities.contains("FEATURE_BETA"));
    assertTrue(authorities.contains("ACCESS_LEVEL_5"));
    assertEquals(5, authorities.size());
  }

  @Test
  void shouldReturnEmptyPasswordForHeadlessAuth() {
    // Given
    User user = User.builder().userId("user-123").username("jack").build();

    // When
    UserDetailsAdapter adapter = new UserDetailsAdapter(user);

    // Then
    assertEquals("", adapter.getPassword());
  }

  @Test
  void shouldAlwaysEnableAccount() {
    // Given
    User user = User.builder().userId("user-123").username("kate").build();

    // When
    UserDetailsAdapter adapter = new UserDetailsAdapter(user);

    // Then
    assertTrue(adapter.isEnabled());
    assertTrue(adapter.isAccountNonExpired());
    assertTrue(adapter.isAccountNonLocked());
    assertTrue(adapter.isCredentialsNonExpired());
  }
}
