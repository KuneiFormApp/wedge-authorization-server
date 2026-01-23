package com.kuneiform.domain.exception;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class UserProviderExceptionTest {

  @Test
  void shouldCreateExceptionWithErrorCodesAndMessages() {
    // Given
    List<String> errorCodes = List.of("INVALID_CREDENTIALS", "USER_DISABLED");
    List<String> messages = List.of("Invalid password", "Account is disabled");
    Instant errorDate = Instant.parse("2026-01-22T17:58:14Z");

    // When
    UserProviderException exception = new UserProviderException(errorCodes, messages, errorDate);

    // Then
    assertEquals(errorCodes, exception.getErrorCodes());
    assertEquals(messages, exception.getMessages());
    assertEquals(errorDate, exception.getErrorDate());
    assertTrue(exception.getMessage().contains("INVALID_CREDENTIALS"));
    assertTrue(exception.getMessage().contains("USER_DISABLED"));
    assertTrue(exception.getMessage().contains("Invalid password"));
    assertTrue(exception.getMessage().contains("Account is disabled"));
  }

  @Test
  void shouldCreateExceptionWithCause() {
    // Given
    List<String> errorCodes = List.of("SERVICE_UNAVAILABLE");
    List<String> messages = List.of("Service temporarily unavailable");
    Instant errorDate = Instant.now();
    RuntimeException cause = new RuntimeException("Connection timeout");

    // When
    UserProviderException exception =
        new UserProviderException(errorCodes, messages, errorDate, cause);

    // Then
    assertEquals(errorCodes, exception.getErrorCodes());
    assertEquals(messages, exception.getMessages());
    assertEquals(errorDate, exception.getErrorDate());
    assertEquals(cause, exception.getCause());
  }

  @Test
  void shouldHandleEmptyErrorCodesAndMessages() {
    // Given
    List<String> errorCodes = List.of();
    List<String> messages = List.of();
    Instant errorDate = Instant.now();

    // When
    UserProviderException exception = new UserProviderException(errorCodes, messages, errorDate);

    // Then
    assertEquals(errorCodes, exception.getErrorCodes());
    assertEquals(messages, exception.getMessages());
    assertEquals(errorDate, exception.getErrorDate());
    assertEquals("Unknown error from User Provider API", exception.getMessage());
  }

  @Test
  void shouldFormatErrorMessageCorrectly() {
    // Given
    List<String> errorCodes = List.of("INVALID_REQUEST");
    List<String> messages = List.of("Missing required field");
    Instant errorDate = Instant.now();

    // When
    UserProviderException exception = new UserProviderException(errorCodes, messages, errorDate);

    // Then
    String expectedMessage = "User Provider API error [INVALID_REQUEST]: Missing required field";
    assertEquals(expectedMessage, exception.getMessage());
  }

  @Test
  void shouldFormatMultipleErrorCodesAndMessages() {
    // Given
    List<String> errorCodes = List.of("INVALID_REQUEST", "MISSING_USERNAME");
    List<String> messages = List.of("Invalid format", "Username is required");
    Instant errorDate = Instant.now();

    // When
    UserProviderException exception = new UserProviderException(errorCodes, messages, errorDate);

    // Then
    String message = exception.getMessage();
    assertTrue(message.contains("INVALID_REQUEST, MISSING_USERNAME"));
    assertTrue(message.contains("Invalid format, Username is required"));
  }
}
