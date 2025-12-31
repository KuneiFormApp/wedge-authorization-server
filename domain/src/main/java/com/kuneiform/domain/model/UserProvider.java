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
}
