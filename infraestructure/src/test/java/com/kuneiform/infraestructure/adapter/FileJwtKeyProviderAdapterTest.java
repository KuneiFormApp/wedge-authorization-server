package com.kuneiform.infraestructure.adapter;

import static org.junit.jupiter.api.Assertions.*;

import com.kuneiform.domain.model.JwtKeyPair;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FileJwtKeyProviderAdapterTest {

  private Path tempDir;
  private Path privateKeyFile;
  private Path publicKeyFile;

  @BeforeEach
  void setUp() throws IOException, NoSuchAlgorithmException {
    // Create temporary directory and key files for testing
    tempDir = Files.createTempDirectory("jwt-key-test");
    privateKeyFile = tempDir.resolve("private_key.pem");
    publicKeyFile = tempDir.resolve("public_key.pem");

    // Generate a real RSA key pair for testing
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
    keyGen.initialize(2048);
    KeyPair keyPair = keyGen.generateKeyPair();

    // Write private key in PKCS#8 format
    String privateKeyPem =
        "-----BEGIN PRIVATE KEY-----\n"
            + Base64.getMimeEncoder(64, "\n".getBytes())
                .encodeToString(keyPair.getPrivate().getEncoded())
            + "\n-----END PRIVATE KEY-----\n";
    Files.writeString(privateKeyFile, privateKeyPem);

    // Write public key in X.509 format
    String publicKeyPem =
        "-----BEGIN PUBLIC KEY-----\n"
            + Base64.getMimeEncoder(64, "\n".getBytes())
                .encodeToString(keyPair.getPublic().getEncoded())
            + "\n-----END PUBLIC KEY-----\n";
    Files.writeString(publicKeyFile, publicKeyPem);
  }

  @AfterEach
  void tearDown() throws IOException {
    // Clean up temporary files
    if (Files.exists(privateKeyFile)) {
      Files.delete(privateKeyFile);
    }
    if (Files.exists(publicKeyFile)) {
      Files.delete(publicKeyFile);
    }
    if (Files.exists(tempDir)) {
      Files.delete(tempDir);
    }
  }

  @Test
  void shouldLoadKeyPairFromValidPemFiles() {
    // Given
    String keyId = "test-key-id";
    FileJwtKeyProviderAdapter adapter =
        new FileJwtKeyProviderAdapter(privateKeyFile.toString(), publicKeyFile.toString(), keyId);

    // When
    JwtKeyPair keyPair = adapter.getKeyPair();

    // Then
    assertNotNull(keyPair);
    assertNotNull(keyPair.getPublicKey());
    assertNotNull(keyPair.getPrivateKey());
    assertNotNull(keyPair.getKeyId());
  }

  @Test
  void shouldUseConfiguredKeyId() {
    // Given
    String expectedKeyId = "my-custom-key-id";
    FileJwtKeyProviderAdapter adapter =
        new FileJwtKeyProviderAdapter(
            privateKeyFile.toString(), publicKeyFile.toString(), expectedKeyId);

    // When
    JwtKeyPair keyPair = adapter.getKeyPair();

    // Then
    assertEquals(expectedKeyId, keyPair.getKeyId(), "Key ID should match the configured value");
  }

  @Test
  void shouldCacheLoadedKeyPair() {
    // Given
    FileJwtKeyProviderAdapter adapter =
        new FileJwtKeyProviderAdapter(
            privateKeyFile.toString(), publicKeyFile.toString(), "test-key");

    // When
    JwtKeyPair keyPair1 = adapter.getKeyPair();
    JwtKeyPair keyPair2 = adapter.getKeyPair();

    // Then
    assertSame(keyPair1, keyPair2, "Should return the same cached instance");
  }

  @Test
  void shouldThrowExceptionWhenPrivateKeyFileNotFound() {
    // Given
    String nonExistentPath = tempDir.resolve("non-existent-private.pem").toString();

    // When & Then
    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () ->
                new FileJwtKeyProviderAdapter(
                    nonExistentPath, publicKeyFile.toString(), "test-key"));

    assertTrue(
        exception.getMessage().contains("Failed to load JWT key pair from files"),
        "Exception message should indicate failure to load keys");
  }

  @Test
  void shouldThrowExceptionWhenPublicKeyFileNotFound() {
    // Given
    String nonExistentPath = tempDir.resolve("non-existent-public.pem").toString();

    // When & Then
    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () ->
                new FileJwtKeyProviderAdapter(
                    privateKeyFile.toString(), nonExistentPath, "test-key"));

    assertTrue(
        exception.getMessage().contains("Failed to load JWT key pair from files"),
        "Exception message should indicate failure to load keys");
  }

  @Test
  void shouldThrowExceptionWhenPrivateKeyHasInvalidFormat() throws IOException {
    // Given
    Files.writeString(privateKeyFile, "INVALID KEY CONTENT");

    // When & Then
    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () ->
                new FileJwtKeyProviderAdapter(
                    privateKeyFile.toString(), publicKeyFile.toString(), "test-key"));

    assertTrue(
        exception.getMessage().contains("Failed to load JWT key pair from files"),
        "Exception message should indicate failure to load keys");
  }

  @Test
  void shouldThrowExceptionWhenPublicKeyHasInvalidFormat() throws IOException {
    // Given
    Files.writeString(publicKeyFile, "INVALID KEY CONTENT");

    // When & Then
    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () ->
                new FileJwtKeyProviderAdapter(
                    privateKeyFile.toString(), publicKeyFile.toString(), "test-key"));

    assertTrue(
        exception.getMessage().contains("Failed to load JWT key pair from files"),
        "Exception message should indicate failure to load keys");
  }
}
