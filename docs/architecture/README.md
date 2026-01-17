# Architecture Diagrams Index

This directory contains comprehensive architecture diagrams for the WedgeAuth Authorization Server.

## Available Diagrams

### 1. [Hexagonal Architecture Overview](./hexagonal-architecture.md)
**Purpose**: Shows the complete hexagonal architecture with all layers

**Key Topics**:
- Domain, Application, and Infrastructure layers
- Inbound and Outbound adapters
- Spring Framework integration points
- Dependency flow and principles
- Spring "magic" explained (component scanning, conditional beans, DI)

**When to use**: Understanding the overall architecture and how components are organized

---

### 2. [Authentication Flow](./authentication-flow.md)
**Purpose**: Shows how user authentication works from login to token generation

**Key Topics**:
- Form login processing
- Custom `HttpUserAuthenticationProvider`
- External User Provider API integration
- OAuth2 authorization code flow integration
- Token customization with `WedgeTokenCustomizer`
- Principal storage (`User` object as `Authentication.principal`)

**When to use**: Understanding how users log in and how authentication integrates with OAuth2

---

### 3. [OAuth2 Authorization Code Flow](./oauth2-flow.md)
**Purpose**: Complete OAuth2 authorization code flow with PKCE

**Key Topics**:
- PKCE generation and validation
- Authorization request processing
- Authorization code generation
- Token exchange
- Access token and refresh token generation
- Token customization
- Token lifecycle management

**When to use**: Understanding the complete OAuth2 flow from client perspective

---

### 4. [Logout Flow](./logout-flow.md)
**Purpose**: Shows how logout works with token revocation

**Key Topics**:
- Spring Security `LogoutFilter` integration
- Custom `OAuth2AuthorizationRevocationLogoutHandler`
- Token revocation process
- Principal indexing for userId lookup
- Refresh token vs access token handling

**When to use**: Understanding how logout revokes tokens and clears sessions

---

### 5. [Logout Class Diagram](./logout-class-diagram.md)
**Purpose**: Shows relationships between logout-related classes

**Key Topics**:
- `OAuth2AuthorizationRevocationLogoutHandler` structure
- `OAuth2AuthorizationService` adapters (in-memory and Redis)
- Principal indexing implementation
- `User` domain model
- Spring annotations and conditional beans

**When to use**: Understanding class relationships and component responsibilities

---

## Diagram Conventions

### Colors

- **Blue Border**: Spring Framework components (auto-configured, "magic")
- **Green**: Inbound adapters (driving side - receive requests)
- **Yellow**: Outbound adapters (driven side - call external systems)
- **Red**: Domain layer (pure business logic, no framework dependencies)
- **Light Blue**: Application layer (use cases, orchestration)

### Annotations

- **`<<interface>>`**: Java interface
- **`<<@Component>>`**: Spring component (auto-discovered)
- **`<<@Configuration>>`**: Spring configuration class
- **`<<@Bean>>`**: Spring bean method
- **`<<@ConditionalOnProperty>>`**: Conditional bean creation
- **`<<@Value @Builder>>`**: Lombok annotations

### Arrows

- **Solid arrow (→)**: Direct method call or dependency
- **Dashed arrow (-.->)**: Implements interface or Spring calls
- **Note boxes**: Explain Spring "magic" or important details

---

## Spring Framework Integration Points

All diagrams highlight where Spring Framework "magic" happens:

1. **Component Scanning**: How Spring finds and creates beans
2. **Dependency Injection**: How Spring wires dependencies
3. **Filter Chain**: How Spring Security intercepts requests
4. **Conditional Beans**: How beans are created based on configuration
5. **Interface Discovery**: How Spring finds implementations
6. **Auto-configuration**: How Spring Boot configures defaults

---

## Reading Guide

### For New Developers

1. Start with [Hexagonal Architecture](./hexagonal-architecture.md) to understand the overall structure
2. Read [Authentication Flow](./authentication-flow.md) to see how login works
3. Review [OAuth2 Flow](./oauth2-flow.md) to understand token generation
4. Check [Logout Flow](./logout-flow.md) to see token revocation

### For Understanding Logout

1. [Logout Class Diagram](./logout-class-diagram.md) - See class relationships
2. [Logout Flow](./logout-flow.md) - See the sequence of operations
3. [Hexagonal Architecture](./hexagonal-architecture.md) - See where logout fits in the architecture

### For Understanding OAuth2

1. [OAuth2 Flow](./oauth2-flow.md) - Complete authorization code flow
2. [Authentication Flow](./authentication-flow.md) - How authentication integrates
3. [Hexagonal Architecture](./hexagonal-architecture.md) - See Spring Authorization Server integration

---

## Mermaid Diagrams

All diagrams use [Mermaid](https://mermaid.js.org/) syntax and can be rendered in:
- GitHub (automatic rendering)
- GitLab (automatic rendering)
- VS Code (with Mermaid extension)
- IntelliJ IDEA (with Mermaid plugin)
- Any Markdown viewer with Mermaid support

---

## Contributing

When adding new diagrams:

1. Use Mermaid syntax for consistency
2. Add detailed notes explaining Spring "magic"
3. Use consistent color coding
4. Include both sequence and class diagrams when appropriate
5. Update this README with the new diagram
6. Add cross-references to related diagrams
