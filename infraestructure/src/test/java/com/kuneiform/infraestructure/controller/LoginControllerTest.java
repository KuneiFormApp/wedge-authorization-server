package com.kuneiform.infraestructure.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.kuneiform.boot.WedgeAuthorizationServerStarter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(classes = WedgeAuthorizationServerStarter.class)
@Import(LoginControllerTest.MockMvcConfig.class)
class LoginControllerTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void shouldReturnLoginPageForUnauthenticatedUser() throws Exception {
    mockMvc
        .perform(get("/login"))
        .andExpect(status().isOk())
        .andExpect(view().name("login"))
        .andExpect(content().string(containsString("WedgeAuth")))
        .andExpect(content().string(containsString("username")))
        .andExpect(content().string(containsString("password")));
  }

  @Test
  void shouldDisplayErrorMessageWhenErrorParamPresent() throws Exception {
    mockMvc
        .perform(get("/login").param("error", "true"))
        .andExpect(status().isOk())
        .andExpect(view().name("login"))
        .andExpect(content().string(containsString("Invalid username or password")));
  }

  @Test
  void shouldDisplayErrorMessageInSpanishWhenErrorParamPresent() throws Exception {
    mockMvc
        .perform(get("/login").param("error", "true").param("lang", "es"))
        .andExpect(status().isOk())
        .andExpect(view().name("login"))
        .andExpect(content().string(containsString("Usuario o contraseña inválidos")));
  }

  @Test
  void shouldDisplayLogoutMessageWhenLogoutParamPresent() throws Exception {
    mockMvc
        .perform(get("/login").param("logout", "true"))
        .andExpect(status().isOk())
        .andExpect(view().name("login"))
        .andExpect(content().string(containsString("You have been logged out successfully")));
  }

  @Test
  void shouldDisplayLogoutMessageInSpanishWhenLogoutParamPresent() throws Exception {
    mockMvc
        .perform(get("/login").param("logout", "true").param("lang", "es"))
        .andExpect(status().isOk())
        .andExpect(view().name("login"))
        .andExpect(content().string(containsString("Has cerrado sesión exitosamente")));
  }

  @Test
  @WithMockUser(
      username = "alice",
      roles = {"USER"})
  void shouldAllowAlreadyAuthenticatedUsersToAccessLoginPage() throws Exception {
    // Users should be able to access /login even when already authenticated
    // This is useful for re-authentication or switching accounts
    mockMvc
        .perform(get("/login").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(view().name("login"));
  }

  @Test
  void shouldIncludeCsrfTokenInLoginPage() throws Exception {
    mockMvc
        .perform(get("/login"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("_csrf")));
  }

  @Test
  void shouldReturnLoginPageWithoutQueryParams() throws Exception {
    mockMvc
        .perform(get("/login"))
        .andExpect(status().isOk())
        .andExpect(view().name("login"))
        .andExpect(content().contentType("text/html;charset=UTF-8"));
  }

  @Test
  void shouldSupportLocaleChangeViaQueryParameter() throws Exception {
    mockMvc
        .perform(get("/login").param("lang", "es"))
        .andExpect(status().isOk())
        .andExpect(view().name("login"))
        .andExpect(content().string(containsString("Iniciar Sesión")));
  }

  @TestConfiguration
  static class MockMvcConfig {
    @Bean
    public MockMvc mockMvc(WebApplicationContext context) {
      return MockMvcBuilders.webAppContextSetup(context)
          .apply(SecurityMockMvcConfigurers.springSecurity())
          .build();
    }
  }
}
