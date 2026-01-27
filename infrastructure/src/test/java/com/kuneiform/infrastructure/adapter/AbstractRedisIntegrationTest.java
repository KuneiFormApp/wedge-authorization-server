package com.kuneiform.infrastructure.adapter;

import com.redis.testcontainers.RedisContainer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for integration tests that require a Redis container. Implements the Singleton
 * Container pattern to use a single container instance for all test classes extending this one,
 * optimizing resource usage involved in parallel execution.
 */
// @Testcontainers - Removed to avoid auto-management as we start it manually
public abstract class AbstractRedisIntegrationTest {

  static final RedisContainer REDIS;

  static {
    REDIS = new RedisContainer(DockerImageName.parse("redis:7.2.4-alpine"));
    // Start manually to ensure singleton behavior
    REDIS.start();
  }

  @DynamicPropertySource
  static void redisProperties(DynamicPropertyRegistry registry) {
    // Configure common Redis connection properties
    registry.add("wedge.session.redis.host", REDIS::getHost);
    registry.add("wedge.session.redis.port", REDIS::getFirstMappedPort);

    // Default to empty credentials for Testcontainers
    registry.add("wedge.session.redis.username", () -> "");
    registry.add("wedge.session.redis.password", () -> "");
  }
}
