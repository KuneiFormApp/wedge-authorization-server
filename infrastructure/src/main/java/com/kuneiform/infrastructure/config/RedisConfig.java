package com.kuneiform.infrastructure.config;

import com.kuneiform.domain.model.AuthorizationSession;
import com.kuneiform.infrastructure.config.properties.WedgeConfigProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.ObjectMapper;

/**
 * Redis configuration for session storage.
 *
 * <p>Active only when {@code wedge.session.storage-type=redis}. Uses
 * GenericJackson2JsonRedisSerializer which is compatible with Jackson 3.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "wedge.session.storage-type", havingValue = "redis")
public class RedisConfig {

  private final WedgeConfigProperties properties;

  @Bean
  public RedisConnectionFactory redisConnectionFactory() {
    WedgeConfigProperties.RedisConfig redisConfig = properties.getSession().getRedis();

    RedisStandaloneConfiguration config =
        new RedisStandaloneConfiguration(redisConfig.getHost(), redisConfig.getPort());
    config.setDatabase(redisConfig.getDatabase());

    if (redisConfig.getUsername() != null && !redisConfig.getUsername().isEmpty()) {
      config.setUsername(redisConfig.getUsername());
    }

    if (redisConfig.getPassword() != null && !redisConfig.getPassword().isEmpty()) {
      config.setPassword(redisConfig.getPassword());
    }

    LettuceClientConfiguration.LettuceClientConfigurationBuilder clientConfig =
        LettuceClientConfiguration.builder();

    if (redisConfig.getSsl().isEnabled()) {
      clientConfig.useSsl();
    }

    LettuceConnectionFactory factory = new LettuceConnectionFactory(config, clientConfig.build());

    log.info(
        "Redis connection factory configured: host={}, port={}, database={}, ssl={}",
        redisConfig.getHost(),
        redisConfig.getPort(),
        redisConfig.getDatabase(),
        redisConfig.getSsl().isEnabled());

    return factory;
  }

  @Bean
  public ObjectMapper redisObjectMapper() {
    return tools.jackson.databind.json.JsonMapper.builder()
        .enable(tools.jackson.databind.MapperFeature.INFER_CREATOR_FROM_CONSTRUCTOR_PROPERTIES)
        .build();
  }

  @Bean
  public RedisTemplate<String, AuthorizationSession> redisTemplate(
      RedisConnectionFactory connectionFactory, ObjectMapper redisObjectMapper) {

    RedisTemplate<String, AuthorizationSession> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);

    template.setKeySerializer(new StringRedisSerializer());
    template.setHashKeySerializer(new StringRedisSerializer());

    Jackson3RedisSerializer<AuthorizationSession> valueSerializer =
        new Jackson3RedisSerializer<>(redisObjectMapper, AuthorizationSession.class);

    template.setValueSerializer(valueSerializer);
    template.setHashValueSerializer(valueSerializer);

    template.afterPropertiesSet();

    log.info("RedisTemplate configured correctly for Spring Boot 4 / Jackson 3");

    return template;
  }

  // StringRedisTemplate for simple string operations (e.g., user session index sets).
  @Bean
  public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
    return new StringRedisTemplate(connectionFactory);
  }
}
