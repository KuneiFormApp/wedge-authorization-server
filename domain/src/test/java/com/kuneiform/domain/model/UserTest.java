package com.kuneiform.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class UserTest {

  @Test
  void shouldBuildUser() {
    User user =
        User.builder()
            .userId("user-123")
            .username("testuser")
            .email("test@example.com")
            .metadata(Map.of("firstName", "John", "lastName", "Doe", "roles", List.of("USER")))
            .build();

    assertEquals("user-123", user.getUserId());
    assertEquals("testuser", user.getUsername());
    assertEquals("test@example.com", user.getEmail());
    assertEquals("John", user.getMetadata().get("firstName"));
  }

  @Test
  void shouldDetectRolePresence() {
    User user =
        User.builder()
            .userId("user-123")
            .username("testuser")
            .metadata(Map.of("authorities", List.of("USER", "ADMIN")))
            .build();

    assertTrue(user.hasAuthority("USER"));
    assertTrue(user.hasAuthority("ADMIN"));
    assertFalse(user.hasAuthority("SUPERADMIN"));
  }

  @Test
  void shouldHandleMissingRolesMetadata() {
    User user = User.builder().userId("user-123").username("testuser").metadata(Map.of()).build();

    assertFalse(user.hasAuthority("USER"));
  }

  @Test
  void shouldHandleNullMetadata() {
    User user = User.builder().userId("user-123").username("testuser").metadata(null).build();

    assertFalse(user.hasAuthority("USER"));
  }
}
