package com.kuneiform.infraestructure.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Component;

/**
 * Custom AuthenticationProvider that allows public clients to authenticate specifically for the
 * refresh_token grant without requiring proof (code_verifier).
 *
 * <p>Standard Spring Security PublicClientAuthenticationProvider enforces PKCE checks on all
 * requests from public clients. However, the Refresh Token grant does not use PKCE parameters. This
 * provider bridges that gap by validating the client identity for this specific grant type while
 * ensuring the client is indeed public and allowed to perform this action.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PublicClientRefreshTokenAuthenticationProvider implements AuthenticationProvider {

  private final RegisteredClientRepository registeredClientRepository;

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    OAuth2ClientAuthenticationToken clientAuthentication =
        (OAuth2ClientAuthenticationToken) authentication;

    // Only handle ClientAuthenticationMethod.NONE
    if (!ClientAuthenticationMethod.NONE.equals(
        clientAuthentication.getClientAuthenticationMethod())) {
      return null;
    }

    // Check if this is a refresh_token grant request
    // (This requires our custom converter to have put the grant_type in parameters)
    Object grantType =
        clientAuthentication.getAdditionalParameters().get(OAuth2ParameterNames.GRANT_TYPE);
    if (!AuthorizationGrantType.REFRESH_TOKEN.getValue().equals(grantType)) {
      return null; // Let the standard provider handle other flows (like authorization_code which
      // needs PKCE)
    }

    String clientId = clientAuthentication.getPrincipal().toString();
    RegisteredClient registeredClient = this.registeredClientRepository.findByClientId(clientId);

    if (registeredClient == null) {
      throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_CLIENT);
    }

    if (!registeredClient
        .getClientAuthenticationMethods()
        .contains(ClientAuthenticationMethod.NONE)) {
      throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_CLIENT);
    }

    log.debug("Authenticated public client for refresh token: {}", clientId);

    return new OAuth2ClientAuthenticationToken(
        registeredClient, ClientAuthenticationMethod.NONE, null);
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return OAuth2ClientAuthenticationToken.class.isAssignableFrom(authentication);
  }
}
