package com.kuneiform.infrastructure.advice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.kuneiform.domain.exception.UserProviderException;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

  @Mock private MessageSource messageSource;

  //  @Mock private Model model;

  @Mock private RedirectAttributes redirectAttributes;

  private GlobalExceptionHandler exceptionHandler;

  @BeforeEach
  void setUp() {
    exceptionHandler = new GlobalExceptionHandler(messageSource);
  }

  @Test
  void shouldAlwaysRedirectForUserProviderException() {
    // Given
    List<String> errorCodes = List.of("INVALID_CREDENTIALS");
    UserProviderException exception =
        new UserProviderException(errorCodes, List.of("Invalid credentials"), Instant.now());
    Locale locale = Locale.ENGLISH;

    when(messageSource.getMessage(eq("userprovider.error.INVALID_CREDENTIALS"), any(), eq(locale)))
        .thenReturn("Invalid username or password");

    // When
    String view =
        exceptionHandler.handleUserProviderException(exception, locale, redirectAttributes);

    // Then - should always redirect for proper user experience
    assertEquals("redirect:/login?error", view);
  }
}
