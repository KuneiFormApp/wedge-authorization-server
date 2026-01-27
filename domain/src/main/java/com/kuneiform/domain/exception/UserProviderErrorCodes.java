package com.kuneiform.domain.exception;

/**
 * Error codes for the User Provider API.
 *
 * <p>These codes are used to identify specific error scenarios when communicating with the external
 * user provider service.
 */
public final class UserProviderErrorCodes {

  private UserProviderErrorCodes() {
    // Utility class - prevent instantiation
  }

  // Authentication and validation errors
  public static final String INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
  public static final String USER_NOT_FOUND = "USER_NOT_FOUND";
  public static final String USER_DISABLED = "USER_DISABLED";
  public static final String USER_LOCKED = "USER_LOCKED";
  public static final String USER_EXPIRED = "USER_EXPIRED";

  // Request and validation errors
  public static final String INVALID_REQUEST = "INVALID_REQUEST";
  public static final String MISSING_USERNAME = "MISSING_USERNAME";
  public static final String MISSING_PASSWORD = "MISSING_PASSWORD";
  public static final String INVALID_USERNAME_FORMAT = "INVALID_USERNAME_FORMAT";
  public static final String INVALID_PASSWORD_FORMAT = "INVALID_PASSWORD_FORMAT";

  // MFA related errors
  public static final String MFA_REGISTRATION_FAILED = "MFA_REGISTRATION_FAILED";
  public static final String MFA_INVALID_SECRET = "MFA_INVALID_SECRET";
  public static final String MFA_INVALID_KEY_ID = "MFA_INVALID_KEY_ID";
  public static final String MFA_ALREADY_REGISTERED = "MFA_ALREADY_REGISTERED";
  public static final String MFA_NOT_SUPPORTED = "MFA_NOT_SUPPORTED";

  // System and service errors
  public static final String SERVICE_UNAVAILABLE = "SERVICE_UNAVAILABLE";
  public static final String TIMEOUT_ERROR = "TIMEOUT_ERROR";
  public static final String NETWORK_ERROR = "NETWORK_ERROR";
  public static final String INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";
  public static final String DATABASE_ERROR = "DATABASE_ERROR";

  // Authorization and access errors
  public static final String UNAUTHORIZED_CLIENT = "UNAUTHORIZED_CLIENT";
  public static final String FORBIDDEN_OPERATION = "FORBIDDEN_OPERATION";
  public static final String RATE_LIMIT_EXCEEDED = "RATE_LIMIT_EXCEEDED";

  // Configuration errors
  public static final String INVALID_ENDPOINT = "INVALID_ENDPOINT";
  public static final String MISSING_CONFIGURATION = "MISSING_CONFIGURATION";
}
