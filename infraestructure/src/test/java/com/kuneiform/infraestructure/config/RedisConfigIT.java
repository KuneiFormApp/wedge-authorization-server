package com.kuneiform.infraestructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.redis.testcontainers.RedisContainer;
import io.lettuce.core.ClientOptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(
    classes = {com.kuneiform.infraestructure.config.RedisConfig.class},
    properties = "wedge.session.storage-type=redis")
@org.springframework.boot.context.properties.EnableConfigurationProperties(
    com.kuneiform.infraestructure.config.properties.WedgeConfigProperties.class)
@Testcontainers
class RedisConfigIT {

  @Container
  static final RedisContainer REDIS =
      new RedisContainer(DockerImageName.parse("redis:7.2.4-alpine"));

  @Autowired private ApplicationContext context;

  @Autowired(required = false)
  private RedisConnectionFactory connectionFactory;

  @DynamicPropertySource
  static void redisProperties(DynamicPropertyRegistry registry) {
    registry.add("wedge.session.redis.host", REDIS::getHost);
    registry.add("wedge.session.redis.port", REDIS::getFirstMappedPort);
  }

  @Test
  void contextLoadsAndRedisIsConfigured() {
    assertThat(connectionFactory).isNotNull();
    assertThat(connectionFactory).isInstanceOf(LettuceConnectionFactory.class);

    LettuceConnectionFactory lettuceFactory = (LettuceConnectionFactory) connectionFactory;

    ClientOptions clientOptions =
        lettuceFactory.getClientConfiguration().getClientOptions().orElseThrow();
    assertThat(clientOptions).isNotNull();
  }
}
