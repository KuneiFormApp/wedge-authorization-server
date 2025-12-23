package com.kuneiform.domain.model;

import lombok.Builder;
import lombok.Value;

/**
 * Value object representing user provider configuration for a client. Defines how and where to
 * authenticate users for a specific OAuth client.
 */
@Value
@Builder
public class UserProviderConfig {
  boolean enabled;
  String endpoint;
  int timeout;
}
