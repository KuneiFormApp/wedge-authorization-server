package com.kuneiform.infrastructure.adapter.models;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class UserProviderErrorResponseTest {

  @Test
  void shouldCreateErrorResponseWithBuilder() {
    // Given
    List<String> errorCodes = List.of("INVALID_CREDENTIALS");
    List<String> messages = List.of("Invalid password");
    Instant errorDate = Instant.parse("2026-01-22T17:58:14Z");

    // When
    UserProviderErrorResponse response =
        UserProviderErrorResponse.builder()
            .errorCodes(errorCodes)
            .messages(messages)
            .errorDate(errorDate)
            .build();

    // Then
    assertEquals(errorCodes, response.getErrorCodes());
    assertEquals(messages, response.getMessages());
    assertEquals(errorDate, response.getErrorDate());
  }

  @Test
  void shouldCreateErrorResponseWithNulls() {
    // When
    UserProviderErrorResponse response =
        UserProviderErrorResponse.builder().errorCodes(null).messages(null).errorDate(null).build();

    // Then
    assertNull(response.getErrorCodes());
    assertNull(response.getMessages());
    assertNull(response.getErrorDate());
  }

  @Test
  void shouldHandleMultipleErrors() {
    // Given
    List<String> errorCodes = List.of("INVALID_REQUEST", "MISSING_USERNAME");
    List<String> messages = List.of("Invalid format", "Username is required");
    Instant errorDate = Instant.now();

    // When
    UserProviderErrorResponse response =
        UserProviderErrorResponse.builder()
            .errorCodes(errorCodes)
            .messages(messages)
            .errorDate(errorDate)
            .build();

    // Then
    assertEquals(2, response.getErrorCodes().size());
    assertEquals(2, response.getMessages().size());
    assertEquals("INVALID_REQUEST", response.getErrorCodes().get(0));
    assertEquals("MISSING_USERNAME", response.getErrorCodes().get(1));
    assertEquals("Invalid format", response.getMessages().get(0));
    assertEquals("Username is required", response.getMessages().get(1));
  }
}
