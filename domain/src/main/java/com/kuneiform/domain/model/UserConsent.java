package com.kuneiform.domain.model;

import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * Domain model representing a user's authorization consent for an OAuth client. Tracks which
 * applications a user has authorized and what scopes they granted.
 */
@Value
@Builder
public class UserConsent {
  String clientId; // OAuth client ID
  String userId; // Principal name (user ID)
  List<String> grantedScopes; // Scopes the user authorized

  // Optional metadata from joined OAuthClient
  String clientName;
  String imageUrl;
  String accessUrl;
}
