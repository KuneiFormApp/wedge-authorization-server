package com.kuneiform.infraestructure.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.kuneiform.domain.model.User;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;

/** Tests for WedgeTokenCustomizer. */
@ExtendWith(MockitoExtension.class)
class WedgeTokenCustomizerTest {

  private WedgeTokenCustomizer tokenCustomizer;

  @Mock private JwtEncodingContext context;

  @Mock private JwtClaimsSet.Builder claimsBuilder;

  @Captor private ArgumentCaptor<Consumer<Map<String, Object>>> claimsConsumerCaptor;

  @BeforeEach
  void setUp() {
    tokenCustomizer = new WedgeTokenCustomizer();
  }

  @Test
  void customize_WithUserPrincipal_ShouldAddUserClaims() {
    // Arrange
    Map<String, Object> metadata = new HashMap<>();
    metadata.put("firstName", "John");
    metadata.put("lastName", "Doe");
    metadata.put("authorities", java.util.List.of("ROLE_USER", "ROLE_ADMIN"));

    User user =
        User.builder()
            .userId("user-123")
            .username("johndoe")
            .email("john.doe@example.com")
            .metadata(metadata)
            .build();

    Authentication authentication = new UsernamePasswordAuthenticationToken(user, "password");

    when(context.getPrincipal()).thenReturn(authentication);
    when(context.getTokenType()).thenReturn(new OAuth2TokenType("access_token"));
    when(context.getClaims()).thenReturn(claimsBuilder);
    when(claimsBuilder.claims(any())).thenReturn(claimsBuilder);

    // Act
    tokenCustomizer.customize(context);

    // Assert
    verify(claimsBuilder).claims(claimsConsumerCaptor.capture());

    Map<String, Object> claims = new HashMap<>();
    claimsConsumerCaptor.getValue().accept(claims);

    assertEquals("user-123", claims.get("sub"));
    assertEquals("john.doe@example.com", claims.get("email"));
    assertEquals("johndoe", claims.get("username"));
    assertEquals("John", claims.get("firstName"));
    assertEquals("Doe", claims.get("lastName"));
    assertEquals(java.util.List.of("ROLE_USER", "ROLE_ADMIN"), claims.get("authorities"));
  }

  @Test
  void customize_WithUserPrincipalAndNoEmail_ShouldNotAddEmailClaim() {
    // Arrange
    User user = User.builder().userId("user-123").username("johndoe").email(null).build();

    Authentication authentication = new UsernamePasswordAuthenticationToken(user, "password");

    when(context.getPrincipal()).thenReturn(authentication);
    when(context.getTokenType()).thenReturn(new OAuth2TokenType("access_token"));
    when(context.getClaims()).thenReturn(claimsBuilder);
    when(claimsBuilder.claims(any())).thenReturn(claimsBuilder);

    // Act
    tokenCustomizer.customize(context);

    // Assert
    verify(claimsBuilder).claims(claimsConsumerCaptor.capture());

    Map<String, Object> claims = new HashMap<>();
    claimsConsumerCaptor.getValue().accept(claims);

    assertEquals("user-123", claims.get("sub"));
    assertEquals("johndoe", claims.get("username"));
    assertFalse(claims.containsKey("email"));
  }

  @Test
  void customize_WithUserPrincipalAndNoMetadata_ShouldAddBasicClaims() {
    // Arrange
    User user =
        User.builder()
            .userId("user-123")
            .username("johndoe")
            .email("john@example.com")
            .metadata(null)
            .build();

    Authentication authentication = new UsernamePasswordAuthenticationToken(user, "password");

    when(context.getPrincipal()).thenReturn(authentication);
    when(context.getTokenType()).thenReturn(new OAuth2TokenType("access_token"));
    when(context.getClaims()).thenReturn(claimsBuilder);
    when(claimsBuilder.claims(any())).thenReturn(claimsBuilder);

    // Act
    tokenCustomizer.customize(context);

    // Assert
    verify(claimsBuilder).claims(claimsConsumerCaptor.capture());

    Map<String, Object> claims = new HashMap<>();
    claimsConsumerCaptor.getValue().accept(claims);

    assertEquals("user-123", claims.get("sub"));
    assertEquals("john@example.com", claims.get("email"));
    assertEquals("johndoe", claims.get("username"));
    assertEquals(3, claims.size()); // Only sub, email, and username
  }

  @Test
  void customize_WithNonUserPrincipal_ShouldNotCustomize() {
    // Arrange
    Authentication authentication = new UsernamePasswordAuthenticationToken("username", "password");

    when(context.getPrincipal()).thenReturn(authentication);
    // No need to stub getTokenType() since the method returns early

    // Act
    tokenCustomizer.customize(context);

    // Assert
    verify(context, never()).getClaims();
  }

  @Test
  void customize_WithIdToken_ShouldCustomizeTheSameWay() {
    // Arrange
    User user =
        User.builder().userId("user-123").username("johndoe").email("john@example.com").build();

    Authentication authentication = new UsernamePasswordAuthenticationToken(user, "password");

    when(context.getPrincipal()).thenReturn(authentication);
    when(context.getTokenType()).thenReturn(new OAuth2TokenType(OidcParameterNames.ID_TOKEN));
    when(context.getClaims()).thenReturn(claimsBuilder);
    when(claimsBuilder.claims(any())).thenReturn(claimsBuilder);

    // Act
    tokenCustomizer.customize(context);

    // Assert
    verify(claimsBuilder).claims(claimsConsumerCaptor.capture());

    Map<String, Object> claims = new HashMap<>();
    claimsConsumerCaptor.getValue().accept(claims);

    assertEquals("user-123", claims.get("sub"));
    assertEquals("john@example.com", claims.get("email"));
    assertEquals("johndoe", claims.get("username"));
  }

  @Test
  void customize_WithEmptyMetadata_ShouldAddBasicClaims() {
    // Arrange
    User user =
        User.builder()
            .userId("user-123")
            .username("johndoe")
            .email("john@example.com")
            .metadata(new HashMap<>())
            .build();

    Authentication authentication = new UsernamePasswordAuthenticationToken(user, "password");

    when(context.getPrincipal()).thenReturn(authentication);
    when(context.getTokenType()).thenReturn(new OAuth2TokenType("access_token"));
    when(context.getClaims()).thenReturn(claimsBuilder);
    when(claimsBuilder.claims(any())).thenReturn(claimsBuilder);

    // Act
    tokenCustomizer.customize(context);

    // Assert
    verify(claimsBuilder).claims(claimsConsumerCaptor.capture());

    Map<String, Object> claims = new HashMap<>();
    claimsConsumerCaptor.getValue().accept(claims);

    assertEquals("user-123", claims.get("sub"));
    assertEquals("john@example.com", claims.get("email"));
    assertEquals("johndoe", claims.get("username"));
    assertEquals(3, claims.size());
  }
}
