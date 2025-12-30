package com.kuneiform.infraestructure.controller;

import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Controller for serving the login page. */
@Controller
@RequiredArgsConstructor
public class LoginController {

  private final MessageSource messageSource;

  @GetMapping("/login")
  public String login(
      @RequestParam(required = false) String error,
      @RequestParam(required = false) String logout,
      Locale locale,
      Model model) {

    if (error != null) {
      model.addAttribute("errorMessage", messageSource.getMessage("login.error", null, locale));
    }

    if (logout != null) {
      model.addAttribute(
          "logoutMessage", messageSource.getMessage("login.logout.success", null, locale));
    }

    return "login";
  }
}
