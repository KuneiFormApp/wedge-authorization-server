package com.kuneiform.infraestructure.adapter.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response model for MFA data from external user provider API.
 *
 * <p>Maps to the {@link com.kuneiform.domain.model.MfaData} domain model.
 */
public record MfaDataResponse(
    @JsonProperty("twoFaRegistered") boolean twoFaRegistered,
    @JsonProperty("mfaKeyId") String mfaKeyId,
    @JsonProperty("mfaSecret") String mfaSecret) {}
