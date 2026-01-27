package com.kuneiform.domain.exception;

import java.time.Instant;
import java.util.List;

/**
 * Exception representing client-side errors (4xx) from the User Provider API.
 *
 * <p>These errors are typically expected (e.g., invalid credentials) and should not trigger circuit
 * breaker state transitions.
 */
public class UserProviderClientException extends UserProviderException {

  public UserProviderClientException(
      List<String> errorCodes, List<String> messages, Instant errorDate) {
    super(errorCodes, messages, errorDate);
  }

  public UserProviderClientException(
      List<String> errorCodes, List<String> messages, Instant errorDate, Throwable cause) {
    super(errorCodes, messages, errorDate, cause);
  }
}
