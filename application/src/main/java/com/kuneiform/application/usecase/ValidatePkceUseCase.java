package com.kuneiform.application.usecase;

import com.kuneiform.domain.model.AuthorizationSession;
import com.kuneiform.domain.port.SessionStorage;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Use case for validating PKCE code verifier against stored challenge. */
@Slf4j
@RequiredArgsConstructor
public class ValidatePkceUseCase {

  private final SessionStorage sessionStorage;

  /**
   * Validates the PKCE code verifier.
   *
   * @param authorizationCode the authorization code
   * @param codeVerifier the code verifier
   * @return true if valid or PKCE not required, false otherwise
   */
  public boolean execute(String authorizationCode, String codeVerifier) {
    log.debug("Validating PKCE for authorization code");

    Optional<AuthorizationSession> sessionOpt =
        sessionStorage.findByAuthorizationCode(authorizationCode);

    if (sessionOpt.isEmpty()) {
      log.warn("Session not found for authorization code");
      return false;
    }

    AuthorizationSession session = sessionOpt.get();

    if (!session.requiresPkce()) {
      log.debug("PKCE not required for this session");
      return true;
    }

    if (codeVerifier == null || codeVerifier.isBlank()) {
      log.warn("PKCE required but code_verifier not provided");
      return false;
    }

    String expectedChallenge = session.getCodeChallenge();
    String challengeMethod = session.getCodeChallengeMethod();

    String actualChallenge;
    if ("S256".equals(challengeMethod)) {
      actualChallenge = generateS256Challenge(codeVerifier);
    } else if ("plain".equals(challengeMethod)) {
      actualChallenge = codeVerifier;
    } else {
      log.warn("Unsupported code_challenge_method: {}", challengeMethod);
      return false;
    }

    boolean isValid = expectedChallenge.equals(actualChallenge);

    if (isValid) {
      log.info("PKCE validation successful");
    } else {
      log.warn("PKCE validation failed");
    }

    return isValid;
  }

  private String generateS256Challenge(String verifier) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(verifier.getBytes(StandardCharsets.US_ASCII));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-256 algorithm not available", e);
    }
  }
}
