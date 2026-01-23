# Agent Guidelines for WedgeAuth Development

This document provides comprehensive guidelines for AI agents and developers working on the WedgeAuth OAuth2.1 Authorization Server project.

## Project Overview

**WedgeAuth** is an OAuth2.1 Authorization Server built with:
- **Spring Boot**: 4.0.1 (released December 18, 2025)
- **Java**: 25
- **Architecture**: Hexagonal Architecture (Ports & Adapters)
- **Build Tool**: Gradle with multi-module structure
- **Code Formatting**: Spotless with Google Java Format
- **Lombok**: We are using lombok to reduce boilerplate, we are using @RequiredArgsConstructor for the dependency injection too. In case we have more than one bean, we are relying in the resolution by name of spring instead of a Qualifier
- **Imports**: Use proper imports. DO NOT USE fully qualified class names

## Architecture

### Hexagonal Architecture Structure

The project follows hexagonal architecture with three Gradle modules:

```
wedge-authorization-server/
├── domain/           # Core business logic, entities, and ports (interfaces)
├── application/      # Use cases and application services
└── infrastructure/  # Adapters, controllers, repositories, and external integrations
```

#### Module Responsibilities

**Domain Module** (`domain/`)
- Contains core business entities and value objects
- Defines ports (interfaces) for external dependencies
- No dependencies on frameworks or infrastructure
- Pure business logic with no external coupling

**Application Module** (`application/`)
- Implements use cases and application services
- Orchestrates domain objects to fulfill business requirements
- Depends on domain module only
- Framework-agnostic business workflows

**Infrastructure Module** (`infrastructure/`)
- Implements adapters for ports defined in domain
- Contains Spring Boot configuration, controllers, and repositories
- Handles external integrations (databases, Redis, HTTP clients)
- Depends on both domain and application modules

### Dependency Rules

- **Domain**: No dependencies on other modules
- **Application**: Depends on `domain` only
- **Infrastructure**: Depends on `domain` and `application`

Always respect these boundaries. Never introduce circular dependencies.

## Technology Stack

### Core Framework
- **Spring Boot**: 4.0.1
- **Spring Security**: OAuth2 Authorization Server
- **Java**: 25 with modern language features

### Migration from Spring Boot 3.x

If you encounter outdated information (pre-4.0), consult the official migration guide:
- https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide

Key changes in Spring Boot 4.0:
- Jackson 3.x (Jakarta namespace)
- Updated dependency coordinates
- Configuration property changes
- Security configuration updates

### Database Support

**PostgreSQL Configuration** (Correct Dependencies):
```gradle
testImplementation "org.springframework.boot:spring-boot-testcontainers"
testImplementation "org.testcontainers:testcontainers-junit-jupiter"
testImplementation "org.testcontainers:testcontainers-postgresql"
```

The project supports multiple databases (PostgreSQL, MySQL, H2) with Testcontainers for integration testing.

## Code Quality Standards

### Formatting with Spotless

All code must be formatted using Spotless with Google Java Format:

```gradle
subprojects {
    apply plugin: 'com.diffplug.spotless'

    spotless {
        java {
            lineEndings 'UNIX'
            endWithNewline()
            googleJavaFormat()
            removeUnusedImports()
            target 'src/main/java/**/*.java', 'src/test/java/**/*.java'
            targetExclude 'build/**'
        }
    }
}
```

**Before committing**, always run:
```bash
./gradlew spotlessApply
```

To check formatting without applying:
```bash
./gradlew spotlessCheck
```

### SOLID Principles

All code must adhere to SOLID principles:

1. **Single Responsibility Principle**: Each class should have one reason to change
2. **Open/Closed Principle**: Open for extension, closed for modification
3. **Liskov Substitution Principle**: Subtypes must be substitutable for their base types
4. **Interface Segregation Principle**: Many specific interfaces are better than one general-purpose interface
5. **Dependency Inversion Principle**: Depend on abstractions, not concretions

These principles apply to **both production code and tests**.

## Testing Requirements

### Test Coverage

**All changes must include tests.** This is non-negotiable.

- **Unit Tests**: For domain logic and application services
- **Integration Tests**: For infrastructure adapters and end-to-end flows
- **Test Containers**: For database and Redis integration tests

### Test Quality Standards

1. **Clean Code**: Tests must be as clean and maintainable as production code
2. **SOLID Principles**: Apply SOLID to test code structure
3. **Descriptive Names**: Test method names should clearly describe what is being tested
4. **Arrange-Act-Assert**: Follow the AAA pattern
5. **Independent Tests**: Tests should not depend on execution order
6. **Fast Execution**: Keep tests fast; use mocks appropriately

### Test Naming Convention

Use descriptive test method names:
```java
@Test
void shouldReturnAccessToken_whenCredentialsAreValid() { }

@Test
void shouldThrowException_whenClientIdIsInvalid() { }
```

### Example Test Structure

```java
@Test
void shouldAuthenticateUser_whenCredentialsAreValid() {
    // Arrange
    var username = "testuser";
    var password = "password123";
    var expectedUser = new User(username, encodedPassword);
    when(userRepository.findByUsername(username)).thenReturn(Optional.of(expectedUser));
    
    // Act
    var result = authenticationService.authenticate(username, password);
    
    // Assert
    assertThat(result).isPresent();
    assertThat(result.get().getUsername()).isEqualTo(username);
}
```

## Documentation and Comments

### The Golden Rules of Comments

#### 1. The "What" vs. The "Why"

**Never document what the code does** — the code already shows that.
**Always document why the code does it** — the reasoning behind decisions.

❌ **Bad**:
```java
x = x + 1; // Increments x by one
```

✅ **Good**:
```java
x = x + 1; // Offset to account for the header row
```

#### 2. Prioritize Clean Naming Over Comments

If you need a comment to explain what a variable is, the variable name is too vague.

❌ **Bad**:
```java
int d = 86400; // seconds in a day
```

✅ **Good**:
```java
int SECONDS_IN_A_DAY = 86400;
```

#### 3. The "Code Smell" Rule

If a section of code requires a paragraph of explanation, **the problem is the complexity, not the lack of comments.**

Instead of adding comments, refactor the code into smaller, more readable functions with descriptive names.

#### 4. Focus on Context and Assumptions

Use comments **only** for things the code cannot express:

✅ **When to comment**:
- **External dependencies**: Why a specific API version is required
- **Business logic**: Why a discount is 15% and not 20%
- **Workarounds**: "Bug in library X requires this specific sort order"
- **Non-obvious algorithms**: Complex mathematical or domain-specific logic
- **Security considerations**: Why certain validation is critical
- **Performance trade-offs**: Why a less readable approach was chosen

❌ **When NOT to comment**:
- What a method does (use descriptive method names)
- What a variable stores (use descriptive variable names)
- Standard language features or patterns
- Obvious business logic

### Javadoc Guidelines

Use Javadoc for:
- **Public APIs**: Interfaces and public methods in domain/application layers
- **Complex domain concepts**: When domain terminology needs explanation
- **Port interfaces**: Document contracts and expectations

Example:
```java
/**
 * Authenticates a user against the configured authentication provider.
 * 
 * @param credentials the user credentials containing username and password
 * @param clientId the OAuth client requesting authentication
 * @return authenticated user details
 * @throws AuthenticationException if credentials are invalid or client is not authorized
 */
User authenticate(Credentials credentials, String clientId);
```

## Common Patterns

### Domain Model Example

```java
package com.kuneiform.wedge.domain.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OAuthClient {
    String clientId;
    String clientSecret;
    Set<String> redirectUris;
    Set<String> scopes;
    boolean requireProofKey;
}
```

### Port (Interface) Example

```java
package com.kuneiform.wedge.domain.port;

import com.kuneiform.wedge.domain.model.User;
import java.util.Optional;

public interface UserRepository {
    Optional<User> findByUsername(String username);
    User save(User user);
    void delete(String username);
}
```

### Adapter (Implementation) Example

```java
package com.kuneiform.wedge.infrastructure.adapter.persistence;

import com.kuneiform.wedge.domain.model.User;
import com.kuneiform.wedge.domain.port.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JpaUserRepositoryAdapter implements UserRepository {
    private final SpringDataUserRepository springDataRepository;
    
    @Override
    public Optional<User> findByUsername(String username) {
        return springDataRepository.findByUsername(username)
            .map(this::toDomain);
    }
    
    private User toDomain(UserEntity entity) {
        return User.builder()
            .username(entity.getUsername())
            .email(entity.getEmail())
            .build();
    }
}
```

## Configuration Management

### Environment Variables

Use environment variables for:
- Secrets (JWT keys, database passwords)
- Environment-specific values (URLs, ports)
- Feature flags

### YAML Configuration

Use `application.yaml` for:
- Default values
- Structure and schema definition
- Development defaults

Example:
```yaml
wedge:
  jwt:
    key-id: ${JWT_KEY_ID:default-key-id}
    private-key-path: ${JWT_PRIVATE_KEY_PATH:classpath:keys/private.pem}
    public-key-path: ${JWT_PUBLIC_KEY_PATH:classpath:keys/public.pem}
```

## Security Considerations

1. **Never log sensitive data**: Passwords, tokens, secrets
2. **Use BCrypt for passwords**: Always use `PasswordEncoder`
3. **Validate all inputs**: Especially OAuth parameters
4. **Fail securely**: Default to deny, explicit allow
5. **Audit security changes**: Document security-related decisions

## Git Workflow

1. **Feature branches**: Create branches from `main`
2. **Descriptive commits**: Use conventional commit messages
3. **Format before commit**: Run `./gradlew spotlessApply`
4. **Tests must pass**: Run `./gradlew test` before pushing
5. **Small PRs**: Keep changes focused and reviewable

## Common Commands

### Build and Test
```bash
# Build all modules
./gradlew build

# Run all tests
./gradlew test

# Run specific module tests
./gradlew :domain:test
./gradlew :application:test
./gradlew :infrastructure:test

# Format code
./gradlew spotlessApply

# Check formatting
./gradlew spotlessCheck
```

### Running the Application
```bash
# Run with default profile
./gradlew :infrastructure:bootRun

# Run with specific profile
./gradlew :infrastructure:bootRun --args='--spring.profiles.active=dev'
```

## Troubleshooting

### Common Issues

**Testcontainers Connection Issues**
- Ensure Docker is running
- Check Docker resource limits
- Use stable Docker images (avoid `latest` tags)

**Jackson Serialization Issues**
- Remember Spring Boot 4.0 uses Jackson 3.x
- Configure `ObjectMapper` for Lombok `@Value` and `@Builder`

**Formatting Failures**
- Run `./gradlew spotlessApply` to auto-fix
- Check for non-UTF-8 characters
- Ensure UNIX line endings

## Resources

- [Spring Boot 4.0 Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide)
- [Spring Authorization Server Documentation](https://docs.spring.io/spring-authorization-server/reference/)
- [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/)
- [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)

## Questions?

When in doubt:
1. Check existing code patterns in the project
2. Consult this guide
3. Follow SOLID principles
4. Write tests
5. Keep it simple

---

**Remember**: Clean code, comprehensive tests, and clear architecture are not optional — they are the foundation of maintainable software.
