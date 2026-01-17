# Class Diagram - Logout Components

This diagram shows the relationships between logout-related classes.

```mermaid
classDiagram
    %% Spring Security Interfaces
    class LogoutHandler {
        <<interface>>
        +logout(request, response, authentication)
    }
    
    class Authentication {
        <<Spring Security>>
        +getName() String
        +getPrincipal() Object
        +getAuthorities() Collection
    }
    
    %% Custom Logout Handler
    class OAuth2AuthorizationRevocationLogoutHandler {
        <<@Component>>
        -OAuth2AuthorizationService authorizationService
        +logout(request, response, authentication)
        -findAuthorizationByUserId(userId) OAuth2Authorization
    }
    
    %% Domain Model
    class User {
        <<@Value @Builder>>
        -String userId
        -String username
        -String email
        -Map~String,Object~ metadata
        +hasAuthority(authority) boolean
    }
    
    %% OAuth2 Authorization Service
    class OAuth2AuthorizationService {
        <<interface>>
        +save(authorization)
        +remove(authorization)
        +findById(id) OAuth2Authorization
        +findByToken(token, type) OAuth2Authorization
    }
    
    class InMemoryOAuth2AuthorizationServiceAdapter {
        <<@Component>>
        -Cache~String,OAuth2Authorization~ authorizationCache
        -Cache~String,String~ tokenIndexCache
        -Cache~String,String~ principalIndexCache
        +save(authorization)
        +remove(authorization)
        +findById(id) OAuth2Authorization
        +findByToken(token, type) OAuth2Authorization
        +findByUserId(userId) OAuth2Authorization
        -createTokenIndexes(authorization)
        -removeTokenIndexes(authorization)
    }
    
    class RedisOAuth2AuthorizationServiceAdapter {
        <<@Component @ConditionalOnProperty>>
        -Cache~String,OAuth2Authorization~ localAuthCache
        -Cache~String,String~ localIndexCache
        -Cache~String,String~ principalIndexCache
        -RedisTemplate redisAuthTemplate
        -RedisTemplate redisIndexTemplate
        +save(authorization)
        +remove(authorization)
        +findById(id) OAuth2Authorization
        +findByToken(token, type) OAuth2Authorization
        +findByUserId(userId) OAuth2Authorization
        -buildPrincipalIndexKey(userId) String
    }
    
    class OAuth2Authorization {
        <<Spring Authorization Server>>
        +getId() String
        +getPrincipalName() String
        +getRegisteredClientId() String
        +getAccessToken() Token
        +getRefreshToken() Token
    }
    
    %% Relationships
    LogoutHandler <|.. OAuth2AuthorizationRevocationLogoutHandler : implements
    OAuth2AuthorizationRevocationLogoutHandler --> OAuth2AuthorizationService : uses
    OAuth2AuthorizationRevocationLogoutHandler --> Authentication : receives
    OAuth2AuthorizationRevocationLogoutHandler --> User : extracts from principal
    
    OAuth2AuthorizationService <|.. InMemoryOAuth2AuthorizationServiceAdapter : implements
    OAuth2AuthorizationService <|.. RedisOAuth2AuthorizationServiceAdapter : implements
    
    InMemoryOAuth2AuthorizationServiceAdapter --> OAuth2Authorization : manages
    RedisOAuth2AuthorizationServiceAdapter --> OAuth2Authorization : manages
    
    Authentication --> User : contains as principal
    OAuth2Authorization --> User : stores userId as principalName
    
    note for OAuth2AuthorizationRevocationLogoutHandler "Registered by Spring Security\nvia .logout() configuration\nin SecurityConfig"
    
    note for InMemoryOAuth2AuthorizationServiceAdapter "Active when:\nwedge.session.storage-type=in-memory"
    
    note for RedisOAuth2AuthorizationServiceAdapter "Active when:\nwedge.session.storage-type=redis\n@ConditionalOnProperty"
    
    note for User "Stored as Authentication.principal\nduring login by\nHttpUserAuthenticationProvider"
```

## Component Responsibilities

### OAuth2AuthorizationRevocationLogoutHandler

**Purpose**: Revoke OAuth2 authorizations when user logs out

**Spring Integration**:
- Annotated with `@Component` - Spring auto-discovers
- Implements `LogoutHandler` interface
- Injected into `SecurityConfig.defaultSecurityFilterChain()`:
  ```java
  .logout(logout -> logout
      .addLogoutHandler(logoutHandler) // Spring calls this on /logout
      .logoutSuccessUrl("/login?logout=true"))
  ```

**Key Methods**:
- `logout()`: Called by Spring Security's `LogoutFilter`
- `findAuthorizationByUserId()`: Helper to find authorizations

### InMemoryOAuth2AuthorizationServiceAdapter

**Purpose**: Store OAuth2 authorizations in memory using Caffeine cache

**Spring Integration**:
- Annotated with `@Component`
- Implements `OAuth2AuthorizationService` (Spring Authorization Server interface)
- Active by default when `wedge.session.storage-type=in-memory`

**Caching Strategy**:
- `authorizationCache`: Main storage (authId → OAuth2Authorization)
- `tokenIndexCache`: Token lookup (tokenValue → authId)
- `principalIndexCache`: User lookup (userId → authId) **← NEW for logout**

### RedisOAuth2AuthorizationServiceAdapter

**Purpose**: Store OAuth2 authorizations in Redis with L1/L2 caching

**Spring Integration**:
- Annotated with `@Component`
- Annotated with `@ConditionalOnProperty(name = "wedge.session.storage-type", havingValue = "redis")`
- Only active when Redis is configured
- Spring creates this bean conditionally

**Caching Strategy**:
- L1 Cache: Caffeine (in-memory, fast)
- L2 Cache: Redis (distributed, persistent)
- Same indexing as in-memory adapter

### User (Domain Model)

**Purpose**: Represent authenticated user

**Spring Integration**:
- No Spring dependencies (pure domain model)
- Created by `HttpUserAuthenticationProvider`
- Stored as `Authentication.principal` by Spring Security
- Extracted during logout to get `userId`

**Lombok Annotations**:
- `@Value`: Immutable class
- `@Builder`: Builder pattern
