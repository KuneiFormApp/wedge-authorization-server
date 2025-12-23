package com.kuneiform.infraestructure.config;

import com.kuneiform.application.usecase.AuthenticateUserUseCase;
import com.kuneiform.application.usecase.CreateAuthorizationSessionUseCase;
import com.kuneiform.application.usecase.ValidatePkceUseCase;
import com.kuneiform.domain.port.ClientRepository;
import com.kuneiform.domain.port.SessionStorage;
import com.kuneiform.domain.port.UserProvider;
import com.kuneiform.infraestructure.config.properties.WedgeConfigProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestClient;

@Configuration
public class ApplicationConfig {

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public RestClient restClient(WedgeConfigProperties config) {
    JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
    requestFactory.setReadTimeout(
        java.time.Duration.ofMillis(config.getUserProvider().getTimeout()));

    return RestClient.builder().requestFactory(requestFactory).build();
  }

  @Bean
  public AuthenticateUserUseCase authenticateUserUseCase(UserProvider userProvider) {
    return new AuthenticateUserUseCase(userProvider);
  }

  @Bean
  public CreateAuthorizationSessionUseCase createAuthorizationSessionUseCase(
      ClientRepository clientRepository,
      SessionStorage sessionStorage,
      WedgeConfigProperties config) {
    return new CreateAuthorizationSessionUseCase(
        clientRepository, sessionStorage, config.getSession().getTtl());
  }

  @Bean
  public ValidatePkceUseCase validatePkceUseCase(SessionStorage sessionStorage) {
    return new ValidatePkceUseCase(sessionStorage);
  }
}
