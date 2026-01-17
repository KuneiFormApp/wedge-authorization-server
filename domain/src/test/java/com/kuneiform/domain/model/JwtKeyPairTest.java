package com.kuneiform.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import org.junit.jupiter.api.Test;

class JwtKeyPairTest {

  @Test
  void shouldCreateJwtKeyPair() {
    RSAPublicKey publicKey = mock(RSAPublicKey.class);
    RSAPrivateKey privateKey = mock(RSAPrivateKey.class);
    String keyId = "test-key-id";

    JwtKeyPair keyPair = new JwtKeyPair(publicKey, privateKey, keyId);

    assertThat(keyPair.getPublicKey()).isEqualTo(publicKey);
    assertThat(keyPair.getPrivateKey()).isEqualTo(privateKey);
    assertThat(keyPair.getKeyId()).isEqualTo(keyId);
  }
}
