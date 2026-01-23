package com.kuneiform.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "wedge.circuit-breaker.user-provider")
public class CircuitBreakerProperties {

  private boolean enabled = true;

  private float failureRateThreshold = 50.0f;

  private int minimumNumberOfCalls = 10;

  private int permittedNumberOfCallsInHalfOpenState = 5;

  private long waitDurationInOpenStateMs = 60000L;

  private long slidingWindowSize = 10000L;

  private String slidingWindowType = "count_based";

  private boolean automaticTransitionFromOpenToHalfOpenEnabled = true;
}
