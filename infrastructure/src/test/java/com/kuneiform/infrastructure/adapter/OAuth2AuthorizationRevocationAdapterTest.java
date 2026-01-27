package com.kuneiform.infrastructure.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;

@ExtendWith(MockitoExtension.class)
class OAuth2AuthorizationRevocationAdapterTest {

  @Mock private InMemoryOAuth2AuthorizationServiceAdapter authorizationService;

  private OAuth2AuthorizationRevocationAdapter adapter;

  @BeforeEach
  void setUp() {
    adapter = new OAuth2AuthorizationRevocationAdapter(authorizationService);
  }

  @Test
  void shouldRevokeById() {
    String authId = "auth-1";
    OAuth2Authorization authorization = mock(OAuth2Authorization.class);

    when(authorizationService.findById(authId)).thenReturn(authorization);

    adapter.revokeById(authId);

    verify(authorizationService).remove(authorization);
  }

  @Test
  void shouldIgnoreNullRevocationId() {
    adapter.revokeById(null);
    verifyNoInteractions(authorizationService);
  }

  @Test
  void shouldRevokeByUserId() {
    String userId = "user-1";
    OAuth2Authorization auth1 = mock(OAuth2Authorization.class);
    OAuth2Authorization auth2 =
        mock(
            OAuth2Authorization
                .class); // Mock expects only one per findByUserId in current impl but list handling
    // is generic

    // The adapter calls findByUserId which currently returns a SINGLE authorization in InMemory
    // adapter
    // But the Revocation adapter handles it as a list if findAuthorizationsByUserId is called
    // Wait, let's check the adapter code again.
    // findAuthorizationsByUserId calls authorizationService.findByUserId(userId) and adds to list.

    when(authorizationService.findByUserId(userId)).thenReturn(auth1);

    int count = adapter.revokeByUserId(userId);

    assertThat(count).isEqualTo(1);
    verify(authorizationService).remove(auth1);
  }

  @Test
  void shouldRevokeByUserAndClient() {
    String userId = "user-2";
    String clientId = "client-A";

    OAuth2Authorization auth1 = mock(OAuth2Authorization.class);
    when(auth1.getId()).thenReturn("auth1");
    when(auth1.getRegisteredClientId()).thenReturn(clientId);

    // In current InMemory implementation, it returns only one auth.
    when(authorizationService.findByUserId(userId)).thenReturn(auth1);

    int count = adapter.revokeByUserAndClient(userId, clientId);

    assertThat(count).isEqualTo(1);
    verify(authorizationService).remove(auth1);
  }
}
