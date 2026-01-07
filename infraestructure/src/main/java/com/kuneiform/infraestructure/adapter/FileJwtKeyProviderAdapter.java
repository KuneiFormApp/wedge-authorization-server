package com.kuneiform.infraestructure.adapter;

import com.kuneiform.domain.model.JwtKeyPair;
import com.kuneiform.domain.port.JwtKeyProvider;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import lombok.extern.slf4j.Slf4j;

/**
 * Adapter that loads RSA key pairs from PEM files.
 *
 * <p>This implementation reads RSA keys from the file system, supporting standard PEM formats:
 *
 * <ul>
 *   <li>Private keys in PKCS#8 format
 *   <li>Public keys in X.509 format
 * </ul>
 *
 * <p>This adapter is suitable for production environments where keys are managed externally and
 * should persist across application restarts.
 *
 * <p>File paths are resolved using Java's {@link Paths} API, which handles both Windows and Linux
 * path formats correctly.
 */
@Slf4j
public class FileJwtKeyProviderAdapter implements JwtKeyProvider {

  private final String privateKeyPath;
  private final String publicKeyPath;
  private final String keyId;
  private final JwtKeyPair cachedKeyPair;

  /**
   * Creates a new file-based key provider and loads the key pair immediately.
   *
   * @param privateKeyPath path to the PEM-encoded private key file (PKCS#8 format)
   * @param publicKeyPath path to the PEM-encoded public key file (X.509 format)
   * @param keyId the key identifier to use for the JWT key pair
   * @throws IllegalStateException if key files cannot be read or parsed
   */
  public FileJwtKeyProviderAdapter(String privateKeyPath, String publicKeyPath, String keyId) {
    this.privateKeyPath = privateKeyPath;
    this.publicKeyPath = publicKeyPath;
    this.keyId = keyId;
    this.cachedKeyPair = loadKeyPair();
  }

  @Override
  public JwtKeyPair getKeyPair() {
    return cachedKeyPair;
  }

  private JwtKeyPair loadKeyPair() {
    try {
      log.info("Loading JWT key pair from files:");
      log.info("  Private key: {}", privateKeyPath);
      log.info("  Public key: {}", publicKeyPath);

      RSAPrivateKey privateKey = loadPrivateKey(privateKeyPath);
      RSAPublicKey publicKey = loadPublicKey(publicKeyPath);

      log.info("Successfully loaded RSA key pair with key ID: {}", keyId);
      return new JwtKeyPair(publicKey, privateKey, keyId);
    } catch (Exception ex) {
      throw new IllegalStateException(
          "Failed to load JWT key pair from files. "
              + "Ensure the files exist and are in valid PEM format (PKCS#8 for private key, X.509 for public key).",
          ex);
    }
  }

  private RSAPrivateKey loadPrivateKey(String filePath) throws Exception {
    Path path = Paths.get(filePath);
    if (!Files.exists(path)) {
      throw new IllegalArgumentException("Private key file not found: " + filePath);
    }

    String keyContent = Files.readString(path);
    String privateKeyPEM =
        keyContent
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replaceAll("\\s", "");

    byte[] encoded = Base64.getDecoder().decode(privateKeyPEM);
    PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
    KeyFactory keyFactory = KeyFactory.getInstance("RSA");

    return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
  }

  private RSAPublicKey loadPublicKey(String filePath) throws Exception {
    Path path = Paths.get(filePath);
    if (!Files.exists(path)) {
      throw new IllegalArgumentException("Public key file not found: " + filePath);
    }

    String keyContent = Files.readString(path);
    String publicKeyPEM =
        keyContent
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replaceAll("\\s", "");

    byte[] encoded = Base64.getDecoder().decode(publicKeyPEM);
    X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
    KeyFactory keyFactory = KeyFactory.getInstance("RSA");

    return (RSAPublicKey) keyFactory.generatePublic(keySpec);
  }
}
