package com.kuneiform.infrastructure.advice;

import com.kuneiform.domain.exception.UserProviderClientException;
import com.kuneiform.domain.exception.UserProviderException;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Global exception handler for web controllers.
 *
 * <p>Maps domain exceptions to user-facing messages with i18n support to provide consistent error
 * experiences across all authentication flows.
 */
@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

  private final MessageSource messageSource;

  /**
   * Handles authentication-related exceptions with redirect to prevent form resubmission issues.
   *
   * <p>Authentication failures typically occur during POST requests; redirecting prevents browser
   * refresh from resubmitting credentials and provides a clean error flow.
   */
  @ExceptionHandler({UserProviderException.class, UserProviderClientException.class})
  public String handleUserProviderException(
      UserProviderException exception, Locale locale, RedirectAttributes redirectAttributes) {

    log.warn("UserProviderException occurred: {}", exception.getMessage());

    List<String> errorCodes = exception.getErrorCodes();
    String errorMessage = resolveErrorMessage(errorCodes, locale);

    // Use redirect for form submissions to prevent resubmission issues
    redirectAttributes.addFlashAttribute("errorMessage", errorMessage);
    return "redirect:/login?error";
  }

  /**
   * Resolves the first error code to maintain consistent messaging for complex errors.
   *
   * <p>User provider exceptions may contain multiple error codes, but showing only the first
   * prevents overwhelming users and aligns with typical authentication error flows.
   */
  private String resolveErrorMessage(List<String> errorCodes, Locale locale) {
    if (errorCodes.isEmpty()) {
      return messageSource.getMessage("error.code.user-provider.service-down", null, locale);
    }

    // Try to resolve the first error code to a localized message
    String firstErrorCode = errorCodes.get(0);
    String messageKey = "userprovider.error." + firstErrorCode;

    try {
      return messageSource.getMessage(messageKey, null, locale);
    } catch (Exception e) {
      log.warn("Could not resolve message for error code: {}", firstErrorCode);
      // Fallback to the original message if available, or default error
      return messageSource.getMessage("error.code.user-provider.service-down", null, locale);
    }
  }
}
