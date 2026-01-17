package com.kuneiform.infraestructure.config;

import com.kuneiform.application.usecase.AuthenticateUserUseCase;
import com.kuneiform.application.usecase.CreateAuthorizationSessionUseCase;
import com.kuneiform.application.usecase.ValidatePkceUseCase;
import com.kuneiform.domain.port.ClientRepository;
import com.kuneiform.domain.port.SessionStorage;
import com.kuneiform.domain.port.UserProviderPort;
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
  public RestClient restClient() {
    JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
    requestFactory.setReadTimeout(java.time.Duration.ofMillis(5000));

    return RestClient.builder().requestFactory(requestFactory).build();
  }

  @Bean
  public AuthenticateUserUseCase authenticateUserUseCase(UserProviderPort userProviderPort) {
    return new AuthenticateUserUseCase(userProviderPort);
  }

  @Bean
  public CreateAuthorizationSessionUseCase createAuthorizationSessionUseCase(
      ClientRepository clientRepository,
      SessionStorage sessionStorage,
      WedgeConfigProperties config) {
    return new CreateAuthorizationSessionUseCase(
        clientRepository, sessionStorage, config.getSession().getAuthTtl());
  }

  @Bean
  public ValidatePkceUseCase validatePkceUseCase(SessionStorage sessionStorage) {
    return new ValidatePkceUseCase(sessionStorage);
  }
}
