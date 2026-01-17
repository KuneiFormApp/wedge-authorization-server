package com.kuneiform.infraestructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.session.data.redis.RedisIndexedSessionRepository;

class HttpSessionConfigTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(HttpSessionConfig.class));

  @Test
  void shouldNotLoadWhenPropertyMissing() {
    contextRunner.run(
        context -> {
          assertThat(context).doesNotHaveBean(HttpSessionConfig.class);
          assertThat(context).doesNotHaveBean(RedisIndexedSessionRepository.class);
        });
  }

  @Test
  void shouldNotLoadWhenPropertyMismatch() {
    contextRunner
        .withPropertyValues("wedge.session.storage-type=jdbc")
        .run(
            context -> {
              assertThat(context).doesNotHaveBean(HttpSessionConfig.class);
            });
  }

  // To test "load when property matches", we need a RedisConnectionFactory bean.
  // HttpSessionConfig requires @EnableRedisHttpSession which looks for RedisConnectionFactory.
  // We can provide a mock one.

  @Test
  void shouldLoadWhenPropertyMatches() {
    contextRunner
        .withPropertyValues("wedge.session.storage-type=redis")
        .withBean(
            RedisConnectionFactory.class,
            () -> org.mockito.Mockito.mock(RedisConnectionFactory.class))
        .run(
            context -> {
              assertThat(context).hasSingleBean(HttpSessionConfig.class);
              // @EnableRedisHttpSession registers a SessionRepository
              assertThat(context).hasBean("sessionRepository");

              // Check by interface
              assertThat(context)
                  .getBean(org.springframework.session.SessionRepository.class)
                  .isNotNull();
            });
  }
}
