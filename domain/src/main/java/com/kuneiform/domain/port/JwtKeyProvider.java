package com.kuneiform.domain.port;

import com.kuneiform.domain.model.JwtKeyPair;

/**
 * Port interface for providing JWT key pairs.
 *
 * <p>This interface defines the contract for obtaining RSA key pairs used in JWT signing and
 * verification. Different implementations can provide keys from various sources (runtime
 * generation, file system, key management services, etc.).
 *
 * <p>Following hexagonal architecture, this port allows the domain to remain independent of the
 * specific key provisioning mechanism.
 */
public interface JwtKeyProvider {

  /**
   * Retrieves the JWT key pair.
   *
   * @return the RSA key pair with public key, private key, and key ID
   * @throws IllegalStateException if the key pair cannot be provided
   */
  JwtKeyPair getKeyPair();
}
