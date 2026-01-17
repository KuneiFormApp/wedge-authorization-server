package com.kuneiform.infraestructure.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.kuneiform.domain.model.OAuthClient;
import com.kuneiform.domain.model.UserConsent;
import com.kuneiform.domain.port.ClientRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InMemoryConsentStorageAdapterTest {

  @Mock private ClientRepository clientRepository;

  private InMemoryConsentStorageAdapter adapter;

  @BeforeEach
  void setUp() {
    adapter = new InMemoryConsentStorageAdapter(clientRepository);
  }

  @Test
  void shouldSaveAndRetrieveConsents() {
    String userId = "user1";
    String clientId = "client1";
    List<String> scopes = List.of("openid", "profile");

    // Setup client mock
    OAuthClient client = OAuthClient.builder().clientId(clientId).clientName("My App").build();
    when(clientRepository.findByClientId(clientId)).thenReturn(Optional.of(client));

    // Save
    adapter.saveConsent(userId, clientId, scopes);

    // Verify retrieval
    List<UserConsent> consents = adapter.findByUserId(userId);
    assertThat(consents).hasSize(1);

    UserConsent consent = consents.get(0);
    assertThat(consent.getUserId()).isEqualTo(userId);
    assertThat(consent.getClientId()).isEqualTo(clientId);
    assertThat(consent.getClientName()).isEqualTo("My App");
    assertThat(consent.getGrantedScopes()).containsExactlyElementsOf(scopes);
  }

  @Test
  void shouldReturnEmptyListWhenNoConsentsFound() {
    List<UserConsent> consents = adapter.findByUserId("unknown");
    assertThat(consents).isEmpty();
  }

  @Test
  void shouldRevokeConsent() {
    String userId = "user2";
    String clientId = "client2";

    adapter.saveConsent(userId, clientId, List.of("scope1"));
    assertThat(adapter.hasConsent(userId, clientId)).isTrue();

    adapter.revokeByClientId(userId, clientId);
    assertThat(adapter.hasConsent(userId, clientId)).isFalse();
  }

  @Test
  void shouldHandleMissingClientDetailsGracefully() {
    String userId = "user3";
    String clientId = "client3";

    when(clientRepository.findByClientId(clientId)).thenReturn(Optional.empty());

    adapter.saveConsent(userId, clientId, List.of("scopeA"));

    List<UserConsent> consents = adapter.findByUserId(userId);
    assertThat(consents).hasSize(1);
    assertThat(consents.get(0).getClientName()).isEqualTo(clientId); // Fallback to ID
    assertThat(consents.get(0).getAccessUrl()).isNull();
  }
}
