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
            .metadata(Map.of("roles", List.of("USER", "ADMIN")))
            .build();

    assertTrue(user.hasRole("USER"));
    assertTrue(user.hasRole("ADMIN"));
    assertFalse(user.hasRole("SUPERADMIN"));
  }

  @Test
  void shouldHandleMissingRolesMetadata() {
    User user = User.builder().userId("user-123").username("testuser").metadata(Map.of()).build();

    assertFalse(user.hasRole("USER"));
  }

  @Test
  void shouldHandleNullMetadata() {
    User user = User.builder().userId("user-123").username("testuser").metadata(null).build();

    assertFalse(user.hasRole("USER"));
  }
}
