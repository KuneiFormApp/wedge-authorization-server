package com.kuneiform.domain.exception;

import java.time.Instant;
import java.util.List;

/**
 * Exception representing errors from the User Provider API.
 *
 * <p>Contains structured error information including error codes, messages, and timestamp as
 * specified by the API provider contract.
 */
public class UserProviderException extends RuntimeException {

  private final List<String> errorCodes;
  private final List<String> messages;
  private final Instant errorDate;

  public UserProviderException(List<String> errorCodes, List<String> messages, Instant errorDate) {
    super(formatErrorMessage(errorCodes, messages));
    this.errorCodes = errorCodes;
    this.messages = messages;
    this.errorDate = errorDate;
  }

  public UserProviderException(
      List<String> errorCodes, List<String> messages, Instant errorDate, Throwable cause) {
    super(formatErrorMessage(errorCodes, messages), cause);
    this.errorCodes = errorCodes;
    this.messages = messages;
    this.errorDate = errorDate;
  }

  public List<String> getErrorCodes() {
    return errorCodes;
  }

  public List<String> getMessages() {
    return messages;
  }

  public Instant getErrorDate() {
    return errorDate;
  }

  private static String formatErrorMessage(List<String> errorCodes, List<String> messages) {
    if (errorCodes.isEmpty() && messages.isEmpty()) {
      return "Unknown error from User Provider API";
    }
    return String.format(
        "User Provider API error [%s]: %s",
        String.join(", ", errorCodes), String.join(", ", messages));
  }
}
