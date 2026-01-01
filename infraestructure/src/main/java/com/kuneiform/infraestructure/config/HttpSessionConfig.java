package com.kuneiform.infraestructure.config;

import com.kuneiform.infraestructure.config.properties.WedgeConfigProperties;
import java.time.Duration;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.data.redis.RedisIndexedSessionRepository;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * Configuration for HTTP session management using Redis.
 *
 * <p>When {@code wedge.session.storage-type=redis}, this configuration enables Spring Session Data
 * Redis to store HTTP sessions in Redis, allowing session sharing across multiple server instances
 * in a load-balanced environment.
 *
 * <p>This is separate from {@link RedisSessionStorageAdapter}, which handles OAuth2 authorization
 * code storage (PKCE sessions).
 *
 * <h2>Session Types</h2>
 *
 * <ul>
 *   <li><b>HTTP Session (this class):</b> User login state, Spring Security context, accessible via
 *       {@code request.getSession()}
 *   <li><b>OAuth2 Authorization Session:</b> PKCE code challenge storage for token exchange, uses
 *       {@code SessionStorage} domain port
 * </ul>
 */
//TODO: Parametrize EnableRedisHttpSession
@Configuration
@ConditionalOnProperty(name = "wedge.session.storage-type", havingValue = "redis")
@EnableRedisHttpSession(redisNamespace = "${wedge.session.redis.http-namespace:wedge:http:session}")
public class HttpSessionConfig {

  // Spring Session automatically configures:
  // - RedisConnectionFactory (reuses existing Redis config)
  // - Session repository
  // - Session filter
  @Bean
  public BeanPostProcessor redisSessionTimeoutConfigurer(WedgeConfigProperties props) {
    return new BeanPostProcessor() {
      @Override
      public Object postProcessBeforeInitialization(Object bean, String beanName) {
        if (bean instanceof RedisIndexedSessionRepository) {
          ((RedisIndexedSessionRepository) bean)
              .setDefaultMaxInactiveInterval(Duration.ofSeconds(props.getSession().getHttpTtl()));
        }
        return bean;
      }
    };
  }
}
