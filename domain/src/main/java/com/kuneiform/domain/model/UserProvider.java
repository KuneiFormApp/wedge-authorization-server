package com.kuneiform.domain.model;

import lombok.Builder;
import lombok.Value;

/**
 * Value object representing a user provider. Defines how and where to authenticate users for a
 * tenant.
 */
@Value
@Builder
public class UserProvider {
  String endpoint;
  int timeout;

  /**
   * URL template for MFA registration endpoint. The {userId} placeholder will be replaced with the
   * actual user ID. Example: "http://localhost:8080/api/v1/users/{userId}/mfa"
   */
  String mfaRegistrationEndpoint;
}
