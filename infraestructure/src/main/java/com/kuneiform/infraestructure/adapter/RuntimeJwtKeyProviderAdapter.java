package com.kuneiform.infraestructure.adapter;

import com.kuneiform.domain.model.JwtKeyPair;
import com.kuneiform.domain.port.JwtKeyProvider;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

/**
 * Adapter that generates RSA key pairs at runtime.
 *
 * <p>This implementation is suitable for development and testing environments where keys can be
 * regenerated on each application restart. The generated key pair is cached and reused for the
 * lifetime of the application.
 *
 * <p><strong>Warning:</strong> Not recommended for production use as keys will change on restart,
 * invalidating all existing JWTs.
 */
@Slf4j
public class RuntimeJwtKeyProviderAdapter implements JwtKeyProvider {

  private final int keySize;
  private final JwtKeyPair cachedKeyPair;

  /**
   * Creates a new runtime key provider and generates the key pair immediately.
   *
   * @param keySize the RSA key size in bits (e.g., 2048, 4096)
   * @throws IllegalStateException if key generation fails
   */
  public RuntimeJwtKeyProviderAdapter(int keySize) {
    this.keySize = keySize;
    this.cachedKeyPair = generateKeyPair();
  }

  @Override
  public JwtKeyPair getKeyPair() {
    return cachedKeyPair;
  }

  private JwtKeyPair generateKeyPair() {
    try {
      log.info("Generating RSA key pair for JWT signing (key size: {} bits)", keySize);
      KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
      keyPairGenerator.initialize(keySize);
      KeyPair keyPair = keyPairGenerator.generateKeyPair();

      RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
      RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
      String keyId = UUID.randomUUID().toString();

      log.info("Successfully generated RSA key pair with key ID: {}", keyId);
      return new JwtKeyPair(publicKey, privateKey, keyId);
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to generate RSA key pair", ex);
    }
  }
}
