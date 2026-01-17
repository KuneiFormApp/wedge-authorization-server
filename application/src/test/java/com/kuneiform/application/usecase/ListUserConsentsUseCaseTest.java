package com.kuneiform.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.kuneiform.domain.model.UserConsent;
import com.kuneiform.domain.port.ConsentStoragePort;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListUserConsentsUseCaseTest {

  @Mock private ConsentStoragePort consentStorage;

  private ListUserConsentsUseCase useCase;

  @BeforeEach
  void setUp() {
    useCase = new ListUserConsentsUseCase(consentStorage);
  }

  @Test
  void shouldReturnConsentsWhenUserHasConsents() {
    // Given
    String userId = "user-123";

    UserConsent consent1 =
        UserConsent.builder()
            .userId(userId)
            .clientId("client-1")
            .clientName("My App")
            .grantedScopes(List.of("openid", "profile"))
            .build();

    UserConsent consent2 =
        UserConsent.builder()
            .userId(userId)
            .clientId("client-2")
            .clientName("Other App")
            .grantedScopes(List.of("email"))
            .build();

    when(consentStorage.findByUserId(userId)).thenReturn(List.of(consent1, consent2));

    // When
    List<UserConsent> result = useCase.execute(userId);

    // Then
    assertNotNull(result);
    assertEquals(2, result.size());
    assertTrue(result.contains(consent1));
    assertTrue(result.contains(consent2));
    verify(consentStorage).findByUserId(userId);
  }

  @Test
  void shouldReturnEmptyListWhenUserHasNoConsents() {
    // Given
    String userId = "user-no-consents";

    when(consentStorage.findByUserId(userId)).thenReturn(Collections.emptyList());

    // When
    List<UserConsent> result = useCase.execute(userId);

    // Then
    assertNotNull(result);
    assertTrue(result.isEmpty());
    verify(consentStorage).findByUserId(userId);
  }
}
