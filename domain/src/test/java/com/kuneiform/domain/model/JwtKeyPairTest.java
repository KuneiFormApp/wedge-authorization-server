package com.kuneiform.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigInteger;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import org.junit.jupiter.api.Test;

class JwtKeyPairTest {

  @Test
  void shouldCreateJwtKeyPair() {
    RSAPublicKey publicKey =
        new RSAPublicKey() {
          @Override
          public BigInteger getPublicExponent() {
            return null;
          }

          @Override
          public BigInteger getModulus() {
            return null;
          }

          @Override
          public String getAlgorithm() {
            return "RSA";
          }

          @Override
          public String getFormat() {
            return "X.509";
          }

          @Override
          public byte[] getEncoded() {
            return new byte[0];
          }
        };
    RSAPrivateKey privateKey =
        new RSAPrivateKey() {
          @Override
          public BigInteger getPrivateExponent() {
            return null;
          }

          @Override
          public BigInteger getModulus() {
            return null;
          }

          @Override
          public String getAlgorithm() {
            return "RSA";
          }

          @Override
          public String getFormat() {
            return "PKCS#8";
          }

          @Override
          public byte[] getEncoded() {
            return new byte[0];
          }
        };
    String keyId = "test-key-id";

    JwtKeyPair keyPair = new JwtKeyPair(publicKey, privateKey, keyId);

    assertThat(keyPair.getPublicKey()).isEqualTo(publicKey);
    assertThat(keyPair.getPrivateKey()).isEqualTo(privateKey);
    assertThat(keyPair.getKeyId()).isEqualTo(keyId);
  }
}
