package com.kuneiform.infraestructure.config;

import com.kuneiform.domain.port.JwtKeyProvider;
import com.kuneiform.infraestructure.adapter.FileJwtKeyProviderAdapter;
import com.kuneiform.infraestructure.adapter.RuntimeJwtKeyProviderAdapter;
import com.kuneiform.infraestructure.config.properties.WedgeConfigProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Configuration class for JWT key provider.
 *
 * <p>This configuration instantiates the appropriate {@link JwtKeyProvider} implementation based on
 * the {@code wedge.jwt.key-type} configuration property:
 *
 * <ul>
 *   <li><strong>test</strong>: Uses {@link RuntimeJwtKeyProviderAdapter} to generate keys at
 *       runtime (suitable for development/testing)
 *   <li><strong>file</strong>: Uses {@link FileJwtKeyProviderAdapter} to load keys from external
 *       PEM files (suitable for production)
 * </ul>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class JwtKeyProviderConfig {

  private final WedgeConfigProperties config;

  /**
   * Creates the JWT key provider bean based on configuration.
   *
   * @return the configured JWT key provider
   * @throws IllegalArgumentException if configuration is invalid
   */
  @Bean
  public JwtKeyProvider jwtKeyProvider() {
    String keyType = config.getJwt().getKeyType();

    log.info("Configuring JWT key provider with key-type: {}", keyType);

    if ("test".equalsIgnoreCase(keyType)) {
      return createRuntimeKeyProvider();
    } else if ("file".equalsIgnoreCase(keyType)) {
      return createFileKeyProvider();
    } else {
      throw new IllegalArgumentException(
          "Invalid JWT key-type: '" + keyType + "'. Supported values are: 'test', 'file'");
    }
  }

  private JwtKeyProvider createRuntimeKeyProvider() {
    int keySize = config.getJwt().getKeySize();
    log.info("Creating runtime JWT key provider (key size: {} bits)", keySize);
    return new RuntimeJwtKeyProviderAdapter(keySize);
  }

  private JwtKeyProvider createFileKeyProvider() {
    String privateKeyPath = config.getJwt().getPrivateKeyPath();
    String publicKeyPath = config.getJwt().getPublicKeyPath();
    String keyId = config.getJwt().getKeyId();

    // Validate required paths
    if (!StringUtils.hasText(privateKeyPath)) {
      throw new IllegalArgumentException(
          "JWT private-key-path is required when key-type is 'file'. "
              + "Set the JWT_PRIVATE_KEY_PATH environment variable or wedge.jwt.private-key-path property.");
    }

    if (!StringUtils.hasText(publicKeyPath)) {
      throw new IllegalArgumentException(
          "JWT public-key-path is required when key-type is 'file'. "
              + "Set the JWT_PUBLIC_KEY_PATH environment variable or wedge.jwt.public-key-path property.");
    }

    if (!StringUtils.hasText(keyId)) {
      throw new IllegalArgumentException(
          "JWT key-id is required when key-type is 'file'. "
              + "Set the JWT_KEY_ID environment variable or wedge.jwt.key-id property.");
    }

    log.info("Creating file-based JWT key provider");
    log.info("  Private key path: {}", privateKeyPath);
    log.info("  Public key path: {}", publicKeyPath);
    log.info("  Key ID: {}", keyId);

    return new FileJwtKeyProviderAdapter(privateKeyPath, publicKeyPath, keyId);
  }
}
