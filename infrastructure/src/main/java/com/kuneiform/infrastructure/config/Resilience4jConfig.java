package com.kuneiform.infrastructure.config;

import com.kuneiform.domain.exception.UserProviderClientException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Resilience4j circuit breaker.
 *
 * <p>Separates circuit breaker configuration from business logic to ensure authentication service
 * degradation is predictable and doesn't impact overall system stability.
 */
@Configuration
@RequiredArgsConstructor
public class Resilience4jConfig {

  private final CircuitBreakerProperties properties;

  @Bean
  public CircuitBreaker userProviderCircuitBreaker(CircuitBreakerRegistry registry) {
    CircuitBreakerConfig config =
        CircuitBreakerConfig.custom()
            .failureRateThreshold(properties.getFailureRateThreshold())
            .waitDurationInOpenState(Duration.ofMillis(properties.getWaitDurationInOpenStateMs()))
            .slidingWindowSize((int) properties.getSlidingWindowSize())
            .minimumNumberOfCalls(properties.getMinimumNumberOfCalls())
            .permittedNumberOfCallsInHalfOpenState(
                properties.getPermittedNumberOfCallsInHalfOpenState())
            .automaticTransitionFromOpenToHalfOpenEnabled(
                properties.isAutomaticTransitionFromOpenToHalfOpenEnabled())
            .slidingWindowType(convertWindowType(properties.getSlidingWindowType()))
            .ignoreExceptions(UserProviderClientException.class)
            .build();

    return registry.circuitBreaker("userProvider", config);
  }

  private CircuitBreakerConfig.SlidingWindowType convertWindowType(String windowType) {
    return "time_based".equalsIgnoreCase(windowType)
        ? CircuitBreakerConfig.SlidingWindowType.TIME_BASED
        : CircuitBreakerConfig.SlidingWindowType.COUNT_BASED;
  }
}
