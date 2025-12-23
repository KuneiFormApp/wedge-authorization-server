package com.kuneiform.domain.model;

import java.util.Map;
import lombok.Builder;
import lombok.Value;

/**
 * Domain model representing an authenticated user. This is framework-agnostic and contains only
 * business logic.
 */
@Value
@Builder
public class User {
  String userId;
  String username;
  String email;

  /**
   * Additional user metadata that can be mapped into JWT claims. Examples: firstName, lastName,
   * roles, permissions, etc.
   */
  Map<String, Object> metadata;

  public boolean hasRole(String role) {
    if (metadata == null || !metadata.containsKey("roles")) {
      return false;
    }
    Object roles = metadata.get("roles");
    if (roles instanceof Iterable<?>) {
      for (Object r : (Iterable<?>) roles) {
        if (role.equals(r.toString())) {
          return true;
        }
      }
    }
    return false;
  }
}
