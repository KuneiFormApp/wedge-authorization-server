package com.kuneiform.infraestructure.controller;

import com.kuneiform.domain.model.User;
import java.security.Principal;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Custom consent screen controller for OAuth2 authorization. Replaces default Spring Authorization
 * Server consent handling with custom template.
 */
@Slf4j
@Controller
public class ConsentController {

  private final RegisteredClientRepository clientRepository;
  private final OAuth2AuthorizationConsentService consentService;

  public ConsentController(
      RegisteredClientRepository clientRepository,
      OAuth2AuthorizationConsentService consentService) {
    this.clientRepository = clientRepository;
    this.consentService = consentService;
  }

  @GetMapping(value = "/oauth2/consent")
  public String consent(
      Principal principal,
      Model model,
      @RequestParam(OAuth2ParameterNames.CLIENT_ID) String clientId,
      @RequestParam(OAuth2ParameterNames.SCOPE) String scope,
      @RequestParam(OAuth2ParameterNames.STATE) String state) {

    log.debug("Consent request for client: {} scopes: {}", clientId, scope);

    // Get registered client
    RegisteredClient client = clientRepository.findByClientId(clientId);
    if (client == null) {
      log.error("Client not found: {}", clientId);
      return "error";
    }

    // Get previously granted scopes
    Set<String> authorizedScopes = new HashSet<>();
    OAuth2AuthorizationConsent consent =
        consentService.findById(client.getId(), principal.getName());
    if (consent != null) {
      authorizedScopes.addAll(consent.getScopes());
    }

    // Parse requested scopes
    Set<String> scopesToApprove = new LinkedHashSet<>();
    Set<String> previouslyApprovedScopes = new HashSet<>();

    for (String requestedScope : StringUtils.delimitedListToStringArray(scope, " ")) {
      if (authorizedScopes.contains(requestedScope)) {
        previouslyApprovedScopes.add(requestedScope);
      } else {
        scopesToApprove.add(requestedScope);
      }
    }

    // Extract username for display (use username instead of userId)
    String displayName = principal.getName(); // Default to principal name
    if (principal instanceof UsernamePasswordAuthenticationToken authToken) {
      if (authToken.getPrincipal() instanceof User user) {
        displayName = user.getUsername();
      }
    }

    // Prepare model
    model.addAttribute("clientId", clientId);
    model.addAttribute("clientName", client.getClientName());
    model.addAttribute("state", state);
    model.addAttribute("scopes", scopesToApprove);
    model.addAttribute("previouslyApprovedScopes", previouslyApprovedScopes);
    model.addAttribute("principalName", displayName);

    // Optional: Client logo/image URL (from client metadata if available)
    if (client.getClientSettings().getSettings().containsKey("logo_uri")) {
      model.addAttribute("clientLogo", client.getClientSettings().getSetting("logo_uri"));
    }

    log.info(
        "Displaying consent page for user: {} client: {} new scopes: {} previously approved: {}",
        displayName,
        clientId,
        scopesToApprove.size(),
        previouslyApprovedScopes.size());

    return "consent";
  }
}
