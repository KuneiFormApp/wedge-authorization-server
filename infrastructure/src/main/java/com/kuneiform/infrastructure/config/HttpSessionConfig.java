package com.kuneiform.infrastructure.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.data.redis.config.ConfigureRedisAction;
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
 *
 * <h2>Configuration via Properties</h2>
 *
 * <p>Namespace and timeout are configured via standard Spring Session properties in
 * application.yaml:
 *
 * <ul>
 *   <li>{@code spring.session.redis.namespace} - Redis key prefix
 *   <li>{@code spring.session.timeout} - Session idle timeout
 * </ul>
 */
@Configuration
@ConditionalOnProperty(name = "wedge.session.storage-type", havingValue = "redis")
@EnableRedisHttpSession
public class HttpSessionConfig {

  @Bean
  public ConfigureRedisAction configureRedisAction() {
    // Disable CONFIG command for cloud Redis (AWS ElastiCache, Azure Cache, etc.)
    return ConfigureRedisAction.NO_OP;
  }
}
