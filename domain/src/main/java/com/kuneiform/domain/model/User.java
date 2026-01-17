package com.kuneiform.domain.model;

import java.io.Serializable;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

/**
 * Domain model representing an authenticated user. This is framework-agnostic and contains only
 * business logic.
 */
@Value
@Builder
public class User implements Serializable {
  private static final long serialVersionUID = 1L;
  String userId;
  String username;
  String email;

  /**
   * Additional user metadata that can be mapped into JWT claims. Examples: firstName, lastName,
   * roles, permissions, etc.
   */
  Map<String, Object> metadata;

  /**
   * Whether Multi-Factor Authentication is enabled for this user. Null or false means MFA is
   * disabled and authentication proceeds normally after username/password validation.
   */
  Boolean mfaEnabled;

  /** MFA configuration data. Only present when mfaEnabled is true. */
  MfaData mfaData;

  public boolean hasAuthority(String authority) {
    if (metadata == null || !metadata.containsKey("authorities")) {
      return false;
    }
    Object authorities = metadata.get("authorities");
    if (authorities instanceof Iterable<?>) {
      for (Object a : (Iterable<?>) authorities) {
        if (authority.equals(a.toString())) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Override Lombok's generated toString() to return only userId. This prevents the entire User
   * object from being serialized when Spring stores the principal name in database tables.
   */
  @Override
  public String toString() {
    return userId;
  }
}
