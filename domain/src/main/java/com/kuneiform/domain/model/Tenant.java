package com.kuneiform.domain.model;

import lombok.Builder;
import lombok.Value;

/**
 * Domain model representing a tenant. Each tenant has one user provider for authenticating users.
 */
@Value
@Builder
public class Tenant {
  String id;
  String name;
  UserProvider userProvider;
}
