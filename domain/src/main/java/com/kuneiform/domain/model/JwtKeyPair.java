package com.kuneiform.domain.model;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import lombok.Value;

/**
 * Domain model representing a JWT RSA key pair.
 *
 * <p>This immutable value object encapsulates the public and private keys used for JWT signing and
 * verification, along with a unique key identifier.
 */
@Value
public class JwtKeyPair {

  /** The RSA public key used for JWT signature verification. */
  RSAPublicKey publicKey;

  /** The RSA private key used for JWT signing. */
  RSAPrivateKey privateKey;

  /** Unique identifier for this key pair, used in JWK sets. */
  String keyId;
}
