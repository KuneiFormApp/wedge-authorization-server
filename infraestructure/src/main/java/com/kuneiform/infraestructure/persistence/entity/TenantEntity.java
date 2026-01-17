package com.kuneiform.infraestructure.persistence.entity;

import java.time.LocalDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** Spring Data JDBC entity for tenant persistence. Maps to the tenants table. */
@Table("tenants")
public class TenantEntity implements Persistable<String> {

  @Id private String id;

  @Column("name")
  private String name;

  @Column("user_provider_endpoint")
  private String userProviderEndpoint;

  @Column("user_provider_timeout")
  private Integer userProviderTimeout;

  @Column("mfa_registration_endpoint")
  private String mfaRegistrationEndpoint;

  @Column("created_at")
  private LocalDateTime createdAt;

  @Column("updated_at")
  private LocalDateTime updatedAt;

  @org.springframework.data.annotation.Transient private boolean isNew = false;

  public TenantEntity() {}

  public void setIsNew(boolean isNew) {
    this.isNew = isNew;
  }

  @Override
  public boolean isNew() {
    return isNew || id == null;
  }

  // Getters and setters
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
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

  public String getMfaRegistrationEndpoint() {
    return mfaRegistrationEndpoint;
  }

  public void setMfaRegistrationEndpoint(String mfaRegistrationEndpoint) {
    this.mfaRegistrationEndpoint = mfaRegistrationEndpoint;
  }
}
