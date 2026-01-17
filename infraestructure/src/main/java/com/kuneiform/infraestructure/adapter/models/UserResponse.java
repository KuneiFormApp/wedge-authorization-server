package com.kuneiform.infraestructure.adapter.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Response model for user data from external user provider API.
 *
 * <p>This is the expected JSON response structure from the user provider HTTP endpoint. Maps to the
 * {@link com.kuneiform.domain.model.User} domain model.
 */
public record UserResponse(
    @JsonProperty("userId") String userId,
    @JsonProperty("username") String username,
    @JsonProperty("email") String email,
    @JsonProperty("metadata") Map<String, Object> metadata,
    @JsonProperty("mfaEnabled") Boolean mfaEnabled,
    @JsonProperty("mfaData") MfaDataResponse mfaData) {}
