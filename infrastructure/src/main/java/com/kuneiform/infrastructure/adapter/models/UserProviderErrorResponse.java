package com.kuneiform.infrastructure.adapter.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * DTO representing error responses from the User Provider API.
 *
 * <p>Matches the specified error format with errorCodes, messages, and errorDate fields.
 */
@Value
@Builder
public class UserProviderErrorResponse {

  @JsonProperty("errorCodes")
  List<String> errorCodes;

  @JsonProperty("messages")
  List<String> messages;

  @JsonProperty("errorDate")
  Instant errorDate;
}
