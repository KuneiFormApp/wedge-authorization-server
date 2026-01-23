package com.kuneiform.infrastructure.persistence.entity;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** Spring Data JDBC entity for tenant persistence. Maps to the tenants table. */
@Table("tenants")
@Getter
@Setter
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

  @Transient private boolean isNew = false;

  public TenantEntity() {}

  public void setIsNew(boolean isNew) {
    this.isNew = isNew;
  }

  @Override
  public boolean isNew() {
    return isNew || id == null;
  }

  @Override
  public String getId() {
    return id;
  }
}
