package com.kuneiform.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.kuneiform.domain.model.User;
import com.kuneiform.domain.port.UserProvider;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthenticateUserUseCaseTest {

  @Mock private UserProvider userProvider;

  private AuthenticateUserUseCase useCase;

  private static final String TEST_CLIENT_ID = "test-client";

  @BeforeEach
  void setUp() {
    useCase = new AuthenticateUserUseCase(userProvider);
  }

  @Test
  void shouldAuthenticateValidUser() {
    User expectedUser = User.builder().userId("user-123").username("testuser").build();
    when(userProvider.validateCredentials(TEST_CLIENT_ID, "testuser", "password123"))
        .thenReturn(Optional.of(expectedUser));

    Optional<User> result = useCase.execute(TEST_CLIENT_ID, "testuser", "password123");

    assertTrue(result.isPresent());
    assertEquals("user-123", result.get().getUserId());
    verify(userProvider).validateCredentials(TEST_CLIENT_ID, "testuser", "password123");
  }

  @Test
  void shouldRejectInvalidCredentials() {
    when(userProvider.validateCredentials(TEST_CLIENT_ID, "testuser", "wrongpassword"))
        .thenReturn(Optional.empty());

    Optional<User> result = useCase.execute(TEST_CLIENT_ID, "testuser", "wrongpassword");

    assertTrue(result.isEmpty());
  }

  @Test
  void shouldRejectBlankUsername() {
    Optional<User> result = useCase.execute(TEST_CLIENT_ID, "", "password123");

    assertTrue(result.isEmpty());
    verifyNoInteractions(userProvider);
  }

  @Test
  void shouldRejectNullUsername() {
    Optional<User> result = useCase.execute(TEST_CLIENT_ID, null, "password123");

    assertTrue(result.isEmpty());
    verifyNoInteractions(userProvider);
  }

  @Test
  void shouldRejectBlankPassword() {
    Optional<User> result = useCase.execute(TEST_CLIENT_ID, "testuser", "");

    assertTrue(result.isEmpty());
    verifyNoInteractions(userProvider);
  }

  @Test
  void shouldRejectNullPassword() {
    Optional<User> result = useCase.execute(TEST_CLIENT_ID, "testuser", null);

    assertTrue(result.isEmpty());
    verifyNoInteractions(userProvider);
  }

  @Test
  void shouldRejectBlankClientId() {
    Optional<User> result = useCase.execute("", "testuser", "password123");

    assertTrue(result.isEmpty());
    verifyNoInteractions(userProvider);
  }

  @Test
  void shouldRejectNullClientId() {
    Optional<User> result = useCase.execute(null, "testuser", "password123");

    assertTrue(result.isEmpty());
    verifyNoInteractions(userProvider);
  }
}
