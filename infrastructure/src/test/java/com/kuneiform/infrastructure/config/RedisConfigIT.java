package com.kuneiform.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.kuneiform.infrastructure.adapter.AbstractRedisIntegrationTest;
import io.lettuce.core.ClientOptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
    classes = {com.kuneiform.infrastructure.config.RedisConfig.class},
    properties = "wedge.session.storage-type=redis")
@org.springframework.boot.context.properties.EnableConfigurationProperties(
    com.kuneiform.infrastructure.config.properties.WedgeConfigProperties.class)
@Testcontainers
class RedisConfigIT extends AbstractRedisIntegrationTest {

  @Autowired private ApplicationContext context;

  @Autowired(required = false)
  private RedisConnectionFactory connectionFactory;

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
