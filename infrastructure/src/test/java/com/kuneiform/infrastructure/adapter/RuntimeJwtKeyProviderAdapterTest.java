package com.kuneiform.infrastructure.adapter;

import static org.junit.jupiter.api.Assertions.*;

import com.kuneiform.domain.model.JwtKeyPair;
import org.junit.jupiter.api.Test;

class RuntimeJwtKeyProviderAdapterTest {

  @Test
  void shouldGenerateKeyPairWith2048Bits() {
    // Given
    int keySize = 2048;
    RuntimeJwtKeyProviderAdapter adapter = new RuntimeJwtKeyProviderAdapter(keySize);

    // When
    JwtKeyPair keyPair = adapter.getKeyPair();

    // Then
    assertNotNull(keyPair);
    assertNotNull(keyPair.getPublicKey());
    assertNotNull(keyPair.getPrivateKey());
    assertNotNull(keyPair.getKeyId());
    assertEquals(keySize, keyPair.getPublicKey().getModulus().bitLength());
  }

  @Test
  void shouldCacheGeneratedKeyPair() {
    // Given
    RuntimeJwtKeyProviderAdapter adapter = new RuntimeJwtKeyProviderAdapter(2048);

    // When
    JwtKeyPair keyPair1 = adapter.getKeyPair();
    JwtKeyPair keyPair2 = adapter.getKeyPair();

    // Then
    assertSame(keyPair1, keyPair2, "Should return the same cached instance");
  }
}
