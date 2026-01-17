package com.kuneiform.infraestructure.security;

import static org.mockito.Mockito.*;

import com.kuneiform.domain.model.User;
import com.kuneiform.infraestructure.adapter.InMemoryOAuth2AuthorizationServiceAdapter;
import com.kuneiform.infraestructure.adapter.RedisOAuth2AuthorizationServiceAdapter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;

@ExtendWith(MockitoExtension.class)
class OAuth2AuthorizationRevocationLogoutHandlerTest {

  @Mock private OAuth2AuthorizationService authorizationService;

  @Mock private InMemoryOAuth2AuthorizationServiceAdapter inMemoryService;

  @Mock private RedisOAuth2AuthorizationServiceAdapter redisService;

  @Mock private HttpServletRequest request;

  @Mock private HttpServletResponse response;

  @Mock private Authentication authentication;

  @Mock private OAuth2Authorization authorization1;

  @Mock private OAuth2Authorization authorization2;

  private OAuth2AuthorizationRevocationLogoutHandler handler;

  @BeforeEach
  void setUp() {
    handler = new OAuth2AuthorizationRevocationLogoutHandler(authorizationService);
  }

  @Test
  void logout_shouldDoNothing_whenAuthenticationIsNull() {
    // When
    handler.logout(request, response, null);

    // Then
    verifyNoInteractions(authorizationService);
  }

  @Test
  void logout_shouldDoNothing_whenPrincipalIsNull() {
    // Given
    when(authentication.getPrincipal()).thenReturn(null);

    // When
    handler.logout(request, response, authentication);

    // Then
    verifyNoInteractions(authorizationService);
  }

  @Test
  void logout_shouldExtractUserIdFromUserPrincipal() {
    // Given
    User user =
        User.builder()
            .userId("user-123")
            .username("john.doe")
            .email("john@example.com")
            .metadata(new HashMap<>())
            .build();

    when(authentication.getPrincipal()).thenReturn(user);

    OAuth2AuthorizationRevocationLogoutHandler handlerWithInMemory =
        new OAuth2AuthorizationRevocationLogoutHandler(inMemoryService);

    when(inMemoryService.findByUserId("user-123")).thenReturn(null);

    // When
    handlerWithInMemory.logout(request, response, authentication);

    // Then
    verify(inMemoryService).findByUserId("user-123");
  }

  @Test
  void logout_shouldRevokeAllAuthorizationsForUser_inMemory() {
    // Given
    User user =
        User.builder()
            .userId("user-456")
            .username("jane.doe")
            .email("jane@example.com")
            .metadata(new HashMap<>())
            .build();

    when(authentication.getPrincipal()).thenReturn(user);

    OAuth2AuthorizationRevocationLogoutHandler handlerWithInMemory =
        new OAuth2AuthorizationRevocationLogoutHandler(inMemoryService);

    // First call returns authorization1, second call returns authorization2, third
    // returns null
    when(inMemoryService.findByUserId("user-456"))
        .thenReturn(authorization1)
        .thenReturn(authorization2)
        .thenReturn(null);

    when(authorization1.getId()).thenReturn("auth-1");
    when(authorization1.getRegisteredClientId()).thenReturn("client-1");
    when(authorization2.getId()).thenReturn("auth-2");
    when(authorization2.getRegisteredClientId()).thenReturn("client-2");

    // When
    handlerWithInMemory.logout(request, response, authentication);

    // Then
    verify(inMemoryService, times(3)).findByUserId("user-456");
    verify(inMemoryService).remove(authorization1);
    verify(inMemoryService).remove(authorization2);
  }

  @Test
  void logout_shouldRevokeAllAuthorizationsForUser_redis() {
    // Given
    User user =
        User.builder()
            .userId("user-789")
            .username("bob.smith")
            .email("bob@example.com")
            .metadata(new HashMap<>())
            .build();

    when(authentication.getPrincipal()).thenReturn(user);

    OAuth2AuthorizationRevocationLogoutHandler handlerWithRedis =
        new OAuth2AuthorizationRevocationLogoutHandler(redisService);

    when(redisService.findByUserId("user-789")).thenReturn(authorization1).thenReturn(null);

    when(authorization1.getId()).thenReturn("auth-redis-1");
    when(authorization1.getRegisteredClientId()).thenReturn("redis-client");

    // When
    handlerWithRedis.logout(request, response, authentication);

    // Then
    verify(redisService, times(2)).findByUserId("user-789");
    verify(redisService).remove(authorization1);
  }

  @Test
  void logout_shouldFallbackToGetName_whenPrincipalIsNotUser() {
    // Given
    String principalString = "some-principal";
    when(authentication.getPrincipal()).thenReturn(principalString);
    when(authentication.getName()).thenReturn("fallback-user");

    OAuth2AuthorizationRevocationLogoutHandler handlerWithInMemory =
        new OAuth2AuthorizationRevocationLogoutHandler(inMemoryService);

    when(inMemoryService.findByUserId("fallback-user")).thenReturn(null);

    // When
    handlerWithInMemory.logout(request, response, authentication);

    // Then
    verify(authentication).getName();
    verify(inMemoryService).findByUserId("fallback-user");
  }

  @Test
  void logout_shouldDoNothing_whenUserIdIsNull() {
    // Given
    User user =
        User.builder()
            .userId(null) // null userId
            .username("john.doe")
            .email("john@example.com")
            .metadata(new HashMap<>())
            .build();

    when(authentication.getPrincipal()).thenReturn(user);

    // When
    handler.logout(request, response, authentication);

    // Then
    verifyNoInteractions(authorizationService);
  }

  @Test
  void logout_shouldDoNothing_whenUserIdIsBlank() {
    // Given
    User user =
        User.builder()
            .userId("   ") // blank userId
            .username("john.doe")
            .email("john@example.com")
            .metadata(new HashMap<>())
            .build();

    when(authentication.getPrincipal()).thenReturn(user);

    // When
    handler.logout(request, response, authentication);

    // Then
    verifyNoInteractions(authorizationService);
  }

  @Test
  void logout_shouldHandleUnsupportedAuthorizationService() {
    // Given
    User user =
        User.builder()
            .userId("user-999")
            .username("test.user")
            .email("test@example.com")
            .metadata(new HashMap<>())
            .build();

    when(authentication.getPrincipal()).thenReturn(user);

    // authorizationService is a generic mock, not InMemory or Redis
    OAuth2AuthorizationRevocationLogoutHandler handlerWithGeneric =
        new OAuth2AuthorizationRevocationLogoutHandler(authorizationService);

    // When
    handlerWithGeneric.logout(request, response, authentication);

    // Then - should not throw exception, just log warning
    verifyNoInteractions(authorizationService);
  }
}
