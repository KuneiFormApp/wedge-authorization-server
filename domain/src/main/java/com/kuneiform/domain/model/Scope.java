package com.kuneiform.domain.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Scope {
  String name;
  String description;
}
