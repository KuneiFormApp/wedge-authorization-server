package com.kuneiform.infraestructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "wedge.token-storage.type", havingValue = "redis")
public class TokenStorageRedisConfig {

  @Bean
  public RedisTemplate<String, OAuth2Authorization> redisAuthTemplate(
      RedisConnectionFactory connectionFactory) {
    RedisTemplate<String, OAuth2Authorization> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);
    template.setKeySerializer(new StringRedisSerializer());
    // Use JDK Serialization for OAuth2Authorization to handle exact object state safely
    template.setValueSerializer(new JdkSerializationRedisSerializer());
    template.afterPropertiesSet();
    return template;
  }

  @Bean
  public RedisTemplate<String, String> redisIndexTemplate(
      RedisConnectionFactory connectionFactory) {
    RedisTemplate<String, String> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);
    template.setKeySerializer(new StringRedisSerializer());
    template.setValueSerializer(new StringRedisSerializer());
    template.afterPropertiesSet();
    return template;
  }
}
