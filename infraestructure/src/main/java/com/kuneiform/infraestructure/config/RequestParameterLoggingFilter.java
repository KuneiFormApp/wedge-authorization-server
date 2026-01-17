package com.kuneiform.infraestructure.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
public class RequestParameterLoggingFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    if (request.getRequestURI().contains("/oauth2/token")) {
      log.info("Request Debug: {}", request.getRequestURI());
      log.info("   Method: {}", request.getMethod());
      log.info("   Content-Type: {}", request.getContentType());
      log.info(
          "   Auth Context Before: {}", SecurityContextHolder.getContext().getAuthentication());

      log.info("   Headers:");
      Enumeration<String> headerNames = request.getHeaderNames();
      while (headerNames.hasMoreElements()) {
        String headerName = headerNames.nextElement();
        log.info("     {}: {}", headerName, request.getHeader(headerName));
      }

      log.info("   Parameters:");
      request
          .getParameterMap()
          .forEach((key, value) -> log.info("     {}: {}", key, String.join(",", value)));

      if (request.getParameter("client_id") == null) {
        log.warn("   WARNING: client_id parameter is MISSING!");
      } else {
        log.info("   client_id found: {}", request.getParameter("client_id"));
      }
    }

    filterChain.doFilter(request, response);
  }
}
