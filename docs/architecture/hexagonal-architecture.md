# Hexagonal Architecture Overview

This diagram shows the complete hexagonal architecture of the authorization server.

```mermaid
graph TB
    subgraph "External Actors"
        User[User/Browser]
        Client[OAuth2 Client Application]
        UserAPI[External User Provider API]
    end
    
    subgraph "Infrastructure Layer (Adapters)"
        subgraph "Inbound Adapters (Driving)"
            Controller[LoginController<br/>@RestController]
            SecurityConfig[SecurityConfig<br/>@Configuration]
            AuthProvider[HttpUserAuthenticationProvider<br/>@Component]
            LogoutHandler[OAuth2AuthorizationRevocationLogoutHandler<br/>@Component]
            TokenCustomizer[WedgeTokenCustomizer<br/>@Bean]
        end
        
        subgraph "Outbound Adapters (Driven)"
            UserProviderAdapter[HttpUserProviderAdapter<br/>@Component]
            MfaAdapter[HttpMfaRegistrationAdapter<br/>@Component]
            RestClient[UserProviderRestClient<br/>@Component]
            TotpService[GoogleAuthenticatorTotpService<br/>@Component]
            QrService[ZxingQrCodeService<br/>@Component]
            ClientRepoAdapter[YamlClientRepositoryAdapter<br/>@Component]
            TenantRepoAdapter[YamlTenantRepositoryAdapter<br/>@Component]
            AuthServiceAdapter[InMemory/Redis<br/>OAuth2AuthorizationServiceAdapter<br/>@Component]
            SessionAdapter[InMemory/Redis<br/>SessionStorageAdapter<br/>@Component]
            JwtKeyAdapter[Runtime/File<br/>JwtKeyProviderAdapter<br/>@Component]
        end
        
        subgraph "Spring Framework Integration"
            SpringSecurity[Spring Security<br/>Filter Chain]
            SpringAuthServer[Spring Authorization Server<br/>OAuth2 Endpoints]
            SpringBoot[Spring Boot<br/>Auto-configuration]
        end
    end
    
    subgraph "Application Layer (Use Cases)"
        AuthUseCase[AuthenticateUserUseCase<br/>@Component]
        GenerateSecretUC[GenerateMfaSecretUseCase<br/>@Component]
        ValidateTotpUC[ValidateMfaTotpUseCase<br/>@Component]
        GenerateQrUC[GenerateMfaQrCodeUseCase<br/>@Component]
        RegisterMfaUC[RegisterMfaWithProviderUseCase<br/>@Component]
    end
    
    subgraph "Domain Layer (Business Logic)"
        User_Domain[User<br/>@Value @Builder]
        MfaData_Domain[MfaData<br/>@Value @Builder]
        Client_Domain[OAuthClient<br/>@Value @Builder]
        Tenant_Domain[Tenant<br/>@Value @Builder]
        Session_Domain[AuthorizationSession<br/>@Value @Builder]
        
        subgraph "Ports (Interfaces)"
            UserProviderPort[UserProvider<br/><<interface>>]
            TotpPort[TotpService<br/><<interface>>]
            QrPort[QrCodeService<br/><<interface>>]
            MfaRegPort[MfaRegistrationService<br/><<interface>>]
            ClientRepoPort[ClientRepository<br/><<interface>>]
            TenantRepoPort[TenantRepository<br/><<interface>>]
            SessionStoragePort[SessionStorage<br/><<interface>>]
            JwtKeyPort[JwtKeyProvider<br/><<interface>>]
        end
    end
    
    %% External connections
    User -->|HTTP Requests| SpringSecurity
    Client -->|OAuth2 Requests| SpringAuthServer
    
    %% Spring Framework calls Inbound Adapters
    SpringSecurity -.->|"Calls on /login"| AuthProvider
    SpringSecurity -.->|"Calls on /logout"| LogoutHandler
    SpringSecurity -.->|"Routes /mfa/**"| Controller
    SpringAuthServer -.->|"Calls during token generation"| TokenCustomizer
    SpringBoot -.->|"Auto-configures"| SecurityConfig
    
    %% Inbound Adapters call Application Layer
    AuthProvider --> AuthUseCase
    AuthProvider -.->|"Triggers MFA flow"| Controller
    Controller --> GenerateSecretUC
    Controller --> ValidateTotpUC
    Controller --> GenerateQrUC
    Controller --> RegisterMfaUC
    LogoutHandler --> AuthServiceAdapter
    TokenCustomizer --> User_Domain
    
    %% Application Layer calls Ports
    AuthUseCase --> UserProviderPort
    GenerateSecretUC --> TotpPort
    ValidateTotpUC --> TotpPort
    GenerateQrUC --> QrPort
   RegisterMfaUC --> MfaRegPort
    
    %% Outbound Adapters implement Ports
    UserProviderAdapter -.->|implements| UserProviderPort
    TotpService -.->|implements| TotpPort
    QrService -.->|implements| QrPort
    MfaAdapter -.->|implements| MfaRegPort
    MfaAdapter --> RestClient
    UserProviderAdapter --> RestClient
    ClientRepoAdapter -.->|implements| ClientRepoPort
    TenantRepoAdapter -.->|implements| TenantRepoPort
    AuthServiceAdapter -.->|"implements Spring's<br/>OAuth2AuthorizationService"| SessionStoragePort
    SessionAdapter -.->|implements| SessionStoragePort
    JwtKeyAdapter -.->|implements| JwtKeyPort
    
    %% Outbound Adapters call External Systems
    RestClient -->|HTTP POST/PATCH| UserAPI
    
    %% Domain relationships
    User_Domain -.->|contains| MfaData_Domain
    User_Domain -.->|used by| AuthUseCase
    Client_Domain -.->|used by| ClientRepoPort
    Tenant_Domain -.->|used by| TenantRepoPort
    Session_Domain -.->|used by| SessionStoragePort
    
    %% Styling
    classDef springMagic fill:#e1f5ff,stroke:#0066cc,stroke-width:3px
    classDef inbound fill:#d4edda,stroke:#28a745,stroke-width:2px
    classDef outbound fill:#fff3cd,stroke:#ffc107,stroke-width:2px
    classDef domain fill:#f8d7da,stroke:#dc3545,stroke-width:2px
    classDef usecase fill:#d1ecf1,stroke:#17a2b8,stroke-width:2px
    classDef mfa fill:#e7d4f5,stroke:#9b59b6,stroke-width:2px
    
    class SpringSecurity,SpringAuthServer,SpringBoot springMagic
    class Controller,SecurityConfig,AuthProvider,LogoutHandler,TokenCustomizer inbound
    class UserProviderAdapter,ClientRepoAdapter,TenantRepoAdapter,AuthServiceAdapter,SessionAdapter,JwtKeyAdapter outbound
    class User_Domain,Client_Domain,Tenant_Domain,Session_Domain,UserProviderPort,ClientRepoPort,TenantRepoPort,SessionStoragePort,JwtKeyPort domain
    class AuthUseCase usecase
    class MfaData_Domain,TotpPort,QrPort,MfaRegPort,TotpService,QrService,MfaAdapter,RestClient,GenerateSecretUC,ValidateTotpUC,GenerateQrUC,RegisterMfaUC mfa
```

## Layer Responsibilities

### Domain Layer (Red)
**Pure Business Logic - No Framework Dependencies**

- **Models**: `User`, `OAuthClient`, `Tenant`, `AuthorizationSession`
- **Ports (Interfaces)**: Define contracts for infrastructure
- **No Spring annotations** - Framework agnostic
- **No external dependencies** - Only Java and Lombok

### Application Layer (Blue + Purple)
**Use Cases - Orchestrate Domain Logic**

- **Authentication**: `AuthenticateUserUseCase`
- **MFA Use Cases** (Purple):
  - `GenerateMfaSecretUseCase` - Generate TOTP secret
  - `ValidateMfaTotpUseCase` - Validate TOTP codes
  - `GenerateMfaQrCodeUseCase` - Generate QR codes
  - `RegisterMfaWithProviderUseCase` - Register MFA with user provider
- **Minimal Spring**: Only `@Component` for dependency inj ection
- **Calls Ports**: Uses interfaces, not implementations
- **Business workflows**: Coordinates domain objects

### Infrastructure Layer (Green/Yellow)

#### Inbound Adapters (Green) - "Driving Side"
**Receive requests from external actors**

- **Controllers**: Handle HTTP requests
- **Security Components**: Integrate with Spring Security
- **Spring Integration**: Heavy use of Spring annotations
- **Called by Spring**: Framework invokes these components

#### Outbound Adapters (Yellow) - "Driven Side"
**Implement ports to interact with external systems**

- **Repositories**: Data access implementations
- **HTTP Clients**: External API integration
- **Storage Adapters**: Cache/Database implementations
- **Implement Ports**: Provide concrete implementations

### Spring Framework (Blue Border)
**"Magic" Happens Here**

1. **Spring Security Filter Chain**
   - Intercepts all HTTP requests
   - Applies security rules
   - Calls `AuthenticationProvider` beans
   - Calls `LogoutHandler` beans
   - Manages `SecurityContext`

2. **Spring Authorization Server**
   - Provides OAuth2 endpoints
   - Calls `OAuth2TokenCustomizer` beans
   - Uses `OAuth2AuthorizationService` bean
   - Manages OAuth2 flows

3. **Spring Boot Auto-configuration**
   - Scans for `@Component`, `@Configuration`, `@Bean`
   - Creates beans based on `@Conditional` annotations
   - Wires dependencies via constructor injection
   - Configures default behaviors

## Dependency Flow

```
External Actors → Spring Framework → Inbound Adapters → Application Layer → Ports ← Outbound Adapters → External Systems
```

**Key Principle**: Dependencies point inward
- Domain layer has NO dependencies on outer layers
- Application layer depends only on Domain
- Infrastructure depends on Application and Domain
- Spring Framework wires everything together

## Spring "Magic" Explained

### 1. Component Scanning
```java
@Component  // Spring finds and creates bean
@Configuration  // Spring processes configuration
@Bean  // Spring creates and manages bean
```

### 2. Conditional Beans
```java
@ConditionalOnProperty(name = "wedge.session.storage-type", havingValue = "redis")
// Spring only creates this bean if property matches
```

### 3. Dependency Injection
```java
@RequiredArgsConstructor  // Lombok generates constructor
// Spring injects dependencies via constructor
```

### 4. Filter Chain Integration
```java
http.logout(logout -> logout.addLogoutHandler(logoutHandler))
// Spring Security calls logoutHandler.logout() on /logout requests
```

### 5. Interface Implementation Discovery
```java
class MyProvider implements AuthenticationProvider
// Spring Security automatically finds and uses all AuthenticationProvider beans
```
