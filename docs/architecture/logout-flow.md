# Logout Flow Architecture

This diagram shows the complete logout flow with token revocation.

```mermaid
sequenceDiagram
    participant User
    participant Browser
    participant SecurityFilterChain as Spring Security<br/>Filter Chain
    participant LogoutFilter as Spring Security<br/>LogoutFilter
    participant LogoutHandler as OAuth2Authorization<br/>RevocationLogoutHandler
    participant AuthService as OAuth2Authorization<br/>Service (Adapter)
    participant Cache as In-Memory/Redis<br/>Storage

    User->>Browser: Click Logout / Navigate to /logout
    Browser->>SecurityFilterChain: GET /logout
    
    Note over SecurityFilterChain: Spring Security intercepts request
    SecurityFilterChain->>LogoutFilter: Process logout request
    
    Note over LogoutFilter: Spring calls configured<br/>logout handlers
    LogoutFilter->>LogoutHandler: logout(request, response, authentication)
    
    Note over LogoutHandler: Extract userId from<br/>User principal
    LogoutHandler->>LogoutHandler: Cast authentication.getPrincipal()<br/>to User object
    LogoutHandler->>LogoutHandler: Extract userId from User
    
    Note over LogoutHandler: Find all authorizations<br/>for this user
    loop For each authorization
        LogoutHandler->>AuthService: findByUserId(userId)
        AuthService->>Cache: Get authId from<br/>principalIndexCache
        Cache-->>AuthService: Return authId
        AuthService->>Cache: Get authorization from<br/>authorizationCache
        Cache-->>AuthService: Return OAuth2Authorization
        AuthService-->>LogoutHandler: Return OAuth2Authorization
        
        LogoutHandler->>AuthService: remove(authorization)
        AuthService->>Cache: Delete from authorizationCache
        AuthService->>Cache: Delete from tokenIndexCache
        AuthService->>Cache: Delete from principalIndexCache
        Cache-->>AuthService: Deleted
        AuthService-->>LogoutHandler: Removed
    end
    
    LogoutHandler-->>LogoutFilter: Logout complete
    LogoutFilter->>SecurityFilterChain: Clear HTTP session
    SecurityFilterChain->>Browser: Redirect to /login?logout=true
    Browser->>User: Show "Logged out successfully"
```

## Key Points

### Spring Security Integration

1. **LogoutFilter** (Spring Security)
   - Automatically configured by Spring Security
   - Intercepts `/logout` requests
   - Calls all registered `LogoutHandler` beans
   - Configured in `SecurityConfig.defaultSecurityFilterChain()`

2. **Filter Chain** (Spring Security)
   - Processes all HTTP requests
   - Applies security rules
   - Manages authentication state

### Custom Components

1. **OAuth2AuthorizationRevocationLogoutHandler**
   - Custom `LogoutHandler` implementation
   - Registered as Spring `@Component`
   - Injected into logout configuration via constructor

2. **OAuth2AuthorizationService Adapters**
   - `InMemoryOAuth2AuthorizationServiceAdapter`
   - `RedisOAuth2AuthorizationServiceAdapter`
   - Implement principal indexing for userId lookup

### Token Revocation Process

1. **Refresh Tokens**: Immediately revoked (removed from storage)
2. **Access Tokens**: Remain valid until expiration (JWT limitation)
3. **Recommendation**: Use 3-5 minute TTL for access tokens in production
