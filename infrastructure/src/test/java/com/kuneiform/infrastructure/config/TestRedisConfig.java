package com.kuneiform.infrastructure.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

/**
 * Test-specific Redis configuration to disable native connection sharing in Lettuce. This can help
 * prevent connection state issues in certain test environments.
 */
@TestConfiguration
public class TestRedisConfig {

  @Bean
  @Primary // Ensures this bean is preferred over the one in RedisConfig during tests
  public RedisConnectionFactory testRedisConnectionFactory(
      RedisConnectionFactory originalConnectionFactory) {
    if (originalConnectionFactory instanceof LettuceConnectionFactory) {
      LettuceConnectionFactory lettuceConnectionFactory =
          (LettuceConnectionFactory) originalConnectionFactory;
      // Disable native connection sharing to ensure connection isolation per operation,
      // which can resolve issues in concurrent test scenarios or specific CI environments.
      lettuceConnectionFactory.setShareNativeConnection(false);
      return lettuceConnectionFactory;
    }
    return originalConnectionFactory;
  }
}
