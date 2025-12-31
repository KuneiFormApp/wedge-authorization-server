package com.kuneiform.infraestructure.persistence.entity;

import java.time.LocalDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Spring Data JDBC entity for OAuth client persistence. Maps to the oauth_clients table. Uses
 * comma-separated strings for collections to maintain simplicity with Spring Data JDBC.
 */
@Table("oauth_clients")
public class OAuthClientEntity {

  @Id private Long id;

  @Column("client_id")
  private String clientId;

  @Column("client_secret")
  private String clientSecret;

  @Column("client_name")
  private String clientName;

  // Collections stored as comma-separated strings
  @Column("client_authentication_methods")
  private String clientAuthenticationMethods;

  @Column("authorization_grant_types")
  private String authorizationGrantTypes;

  @Column("redirect_uris")
  private String redirectUris;

  @Column("post_logout_redirect_uris")
  private String postLogoutRedirectUris;

  @Column("scopes")
  private String scopes;

  // Client settings
  @Column("require_authorization_consent")
  private Boolean requireAuthorizationConsent;

  @Column("require_pkce")
  private Boolean requirePkce;

  // User provider configuration
  @Column("user_provider_enabled")
  private Boolean userProviderEnabled;

  @Column("user_provider_endpoint")
  private String userProviderEndpoint;

  @Column("user_provider_timeout")
  private Integer userProviderTimeout;

  // Audit fields
  @Column("created_at")
  private LocalDateTime createdAt;

  @Column("updated_at")
  private LocalDateTime updatedAt;

  // Constructors
  public OAuthClientEntity() {}

  // Getters and setters
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public String getClientSecret() {
    return clientSecret;
  }

  public void setClientSecret(String clientSecret) {
    this.clientSecret = clientSecret;
  }

  public String getClientName() {
    return clientName;
  }

  public void setClientName(String clientName) {
    this.clientName = clientName;
  }

  public String getClientAuthenticationMethods() {
    return clientAuthenticationMethods;
  }

  public void setClientAuthenticationMethods(String clientAuthenticationMethods) {
    this.clientAuthenticationMethods = clientAuthenticationMethods;
  }

  public String getAuthorizationGrantTypes() {
    return authorizationGrantTypes;
  }

  public void setAuthorizationGrantTypes(String authorizationGrantTypes) {
    this.authorizationGrantTypes = authorizationGrantTypes;
  }

  public String getRedirectUris() {
    return redirectUris;
  }

  public void setRedirectUris(String redirectUris) {
    this.redirectUris = redirectUris;
  }

  public String getPostLogoutRedirectUris() {
    return postLogoutRedirectUris;
  }

  public void setPostLogoutRedirectUris(String postLogoutRedirectUris) {
    this.postLogoutRedirectUris = postLogoutRedirectUris;
  }

  public String getScopes() {
    return scopes;
  }

  public void setScopes(String scopes) {
    this.scopes = scopes;
  }

  public Boolean getRequireAuthorizationConsent() {
    return requireAuthorizationConsent;
  }

  public void setRequireAuthorizationConsent(Boolean requireAuthorizationConsent) {
    this.requireAuthorizationConsent = requireAuthorizationConsent;
  }

  public Boolean getRequirePkce() {
    return requirePkce;
  }

  public void setRequirePkce(Boolean requirePkce) {
    this.requirePkce = requirePkce;
  }

  public Boolean getUserProviderEnabled() {
    return userProviderEnabled;
  }

  public void setUserProviderEnabled(Boolean userProviderEnabled) {
    this.userProviderEnabled = userProviderEnabled;
  }

  public String getUserProviderEndpoint() {
    return userProviderEndpoint;
  }

  public void setUserProviderEndpoint(String userProviderEndpoint) {
    this.userProviderEndpoint = userProviderEndpoint;
  }

  public Integer getUserProviderTimeout() {
    return userProviderTimeout;
  }

  public void setUserProviderTimeout(Integer userProviderTimeout) {
    this.userProviderTimeout = userProviderTimeout;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}
