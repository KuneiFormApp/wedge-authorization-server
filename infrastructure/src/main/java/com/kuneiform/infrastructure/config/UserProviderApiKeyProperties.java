package com.kuneiform.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "wedge.tenants[0].user-provider.api-key")
public class UserProviderApiKeyProperties {

  private boolean mustBeValidated = false;

  private String headerName = "X-API-Key";

  private String value;
}
