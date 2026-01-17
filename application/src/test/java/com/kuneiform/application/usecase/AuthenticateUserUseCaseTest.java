package com.kuneiform.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.kuneiform.domain.model.User;
import com.kuneiform.domain.port.UserProviderPort;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthenticateUserUseCaseTest {

  @Mock private UserProviderPort userProviderPort;

  private AuthenticateUserUseCase useCase;

  private static final String TEST_CLIENT_ID = "test-client";
  private static final String TEST_TENANT_ID = "test-tenant";

  @BeforeEach
  void setUp() {
    useCase = new AuthenticateUserUseCase(userProviderPort);
  }

  @Test
  void shouldAuthenticateValidUserWithClientId() {
    User expectedUser = User.builder().userId("user-123").username("testuser").build();
    when(userProviderPort.validateCredentials(TEST_CLIENT_ID, null, "testuser", "password123"))
        .thenReturn(Optional.of(expectedUser));

    Optional<User> result = useCase.execute(TEST_CLIENT_ID, null, "testuser", "password123");

    assertTrue(result.isPresent());
    assertEquals("user-123", result.get().getUserId());
    verify(userProviderPort).validateCredentials(TEST_CLIENT_ID, null, "testuser", "password123");
  }

  @Test
  void shouldAuthenticateValidUserWithTenantId() {
    User expectedUser = User.builder().userId("user-456").username("testuser").build();
    when(userProviderPort.validateCredentials(null, TEST_TENANT_ID, "testuser", "password123"))
        .thenReturn(Optional.of(expectedUser));

    Optional<User> result = useCase.execute(null, TEST_TENANT_ID, "testuser", "password123");

    assertTrue(result.isPresent());
    assertEquals("user-456", result.get().getUserId());
    verify(userProviderPort).validateCredentials(null, TEST_TENANT_ID, "testuser", "password123");
  }

  @Test
  void shouldRejectInvalidCredentials() {
    when(userProviderPort.validateCredentials(TEST_CLIENT_ID, null, "testuser", "wrongpassword"))
        .thenReturn(Optional.empty());

    Optional<User> result = useCase.execute(TEST_CLIENT_ID, null, "testuser", "wrongpassword");

    assertTrue(result.isEmpty());
  }

  @Test
  void shouldRejectBlankUsername() {
    Optional<User> result = useCase.execute(TEST_CLIENT_ID, null, "", "password123");

    assertTrue(result.isEmpty());
    verifyNoInteractions(userProviderPort);
  }

  @Test
  void shouldRejectNullUsername() {
    Optional<User> result = useCase.execute(TEST_CLIENT_ID, null, null, "password123");

    assertTrue(result.isEmpty());
    verifyNoInteractions(userProviderPort);
  }

  @Test
  void shouldRejectBlankPassword() {
    Optional<User> result = useCase.execute(TEST_CLIENT_ID, null, "testuser", "");

    assertTrue(result.isEmpty());
    verifyNoInteractions(userProviderPort);
  }

  @Test
  void shouldRejectNullPassword() {
    Optional<User> result = useCase.execute(TEST_CLIENT_ID, null, "testuser", null);

    assertTrue(result.isEmpty());
    verifyNoInteractions(userProviderPort);
  }

  @Test
  void shouldRejectWhenBothClientAndTenantIdAreBlank() {
    Optional<User> result = useCase.execute("", "", "testuser", "password123");

    assertTrue(result.isEmpty());
    verifyNoInteractions(userProviderPort);
  }

  @Test
  void shouldRejectWhenBothClientAndTenantIdAreNull() {
    Optional<User> result = useCase.execute(null, null, "testuser", "password123");

    assertTrue(result.isEmpty());
    verifyNoInteractions(userProviderPort);
  }
}
