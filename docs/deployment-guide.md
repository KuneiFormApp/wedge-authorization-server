# WedgeAuth Deployment Guide

This guide covers different deployment scenarios for the WedgeAuth Authorization Server, from minimal development setups to full production configurations.

## Table of Contents

1. [Deployment Scenarios Overview](#deployment-scenarios-overview)
2. [Scenario 1: Standalone](#scenario-1-standalone)
3. [Scenario 2: Customizable](#scenario-2-customizable)
4. [Scenario 3: Distributed Sessions](#scenario-3-distributed-sessions)
5. [Scenario 4: Database-Backed](#scenario-4-database-backed)
6. [Scenario 5: Full Stack](#scenario-5-full-stack)
7. [Environment Variables Reference](#environment-variables-reference)
8. [Troubleshooting](#troubleshooting)

---

## Deployment Scenarios Overview

WedgeAuth is highly configurable and supports various deployment modes:

| Scenario | Client Storage | Session Storage | Token Storage | JWT Keys | Custom UI | Common Use Cases |
|----------|---------------|-----------------|---------------|----------|-----------|------------------|
| **Standalone** | In-memory (YAML) | In-memory | In-memory | Test (generated) | No | Quick testing, POC, local dev |
| **Customizable** | In-memory (YAML) | In-memory | In-memory | Test (generated) | No | Team dev, custom clients |
| **Distributed Sessions** | In-memory (YAML) | Redis | Redis | Test (generated) | No | Multi-instance, staging, load testing |
| **Database-Backed** | PostgreSQL | Redis | Redis | File-based | Optional | Production-ready, persistent clients |
| **Full Stack** | PostgreSQL | Redis | Redis | File-based | Yes | Enterprise, custom branding, multi-region |

> **💡 Important Note:** The Docker Compose examples in this guide are provided to clearly show how environment variables should be configured. **You don't need to run PostgreSQL or Redis in Docker** - you can simply use `docker run` with environment variables pointing to your existing database and Redis instances. The compose files are just for easy reference and local testing.

---

## Scenario 1: Standalone

**Perfect for:** Quick testing, proof-of-concept, local development, first-time setup

**Can be used in:** Any environment (dev, staging, production for simple use cases)

### Configuration

Everything runs in-memory with default settings. No external dependencies required.

### Docker Run

```bash
docker run -p 9001:9001 wedge-auth:jvm
```

### Docker Compose

```yaml
version: '3.8'

services:
  wedge-auth:
    image: wedge-auth:jvm
    ports:
      - "9001:9001"
    environment:
      # Optional: Override defaults
      WEDGE_AUTH_SERVER_PORT: 9001
      LOGGING_LEVEL_ROOT: INFO
```

### What You Get

- ✅ Two pre-configured OAuth clients (see `application.yaml`)
  - `public-spa-client` - For single-page applications
  - `confidential-backend-client` - For backend services
- ✅ In-memory session storage
- ✅ Auto-generated RSA keys for JWT signing
- ✅ Default login UI (Thymeleaf templates)
- ✅ User provider endpoint: `http://localhost:8081/api/users/validate`

### Limitations

- ⚠️ Sessions lost on restart
- ⚠️ Not suitable for multiple instances
- ⚠️ JWT keys change on restart (invalidates existing tokens)

### Testing

Access the authorization server:
```
http://localhost:9001
```

---

## Scenario 2: Customizable

**Perfect for:** Custom client configurations, team development, integration testing

**Can be used in:** Any environment where you need environment-specific client settings

### Configuration

Same as Minimal, but with environment-specific client configurations.

### Docker Compose

```yaml
version: '3.8'

services:
  wedge-auth:
    image: wedge-auth:jvm
    ports:
      - "9001:9001"
    environment:
      # Server Configuration
      WEDGE_AUTH_SERVER_PORT: 9001
      JWT_ISSUER: http://localhost:9001
      
      # Custom Public Client
      PUBLIC_CLIENT_ID: my-spa-app
      PUBLIC_CLIENT_NAME: My SPA Application
      PUBLIC_CLIENT_REDIRECT_URIS: http://localhost:3000/callback,http://localhost:3000/silent-renew
      PUBLIC_CLIENT_POST_LOGOUT_URIS: http://localhost:3000/
      PUBLIC_CLIENT_SCOPES: openid,profile,email,read,write,offline_access
      
      # Custom Confidential Client
      CONFIDENTIAL_CLIENT_ID: my-backend-service
      CONFIDENTIAL_CLIENT_SECRET: my-super-secret-key
      CONFIDENTIAL_CLIENT_REDIRECT_URIS: http://localhost:8082/callback
      
      # User Provider Configuration
      USER_PROVIDER_ENDPOINT: http://user-service:8081/api/users/validate
      USER_PROVIDER_TIMEOUT: 5000
      
      # Logging
      LOGGING_LEVEL_ROOT: INFO
      LOGGING_LEVEL_APP: DEBUG

  # Mock user service (for testing)
  user-service:
    image: your-user-service:latest
    ports:
      - "8081:8081"
```

### What's Different

- ✅ Customized OAuth clients via environment variables
- ✅ Can connect to external user provider service
- ✅ Enhanced logging for debugging

---

## Scenario 3: Distributed Sessions

**Perfect for:** Multi-instance deployments, horizontal scaling, load balancing

**Can be used in:** Development (team shared sessions), staging, or production

### Configuration

Uses Redis for distributed session and token storage.

### Docker Compose

```yaml
version: '3.8'

services:
  wedge-auth:
    image: wedge-auth:jvm
    ports:
      - "9001:9001"
    depends_on:
      - redis
    environment:
      # Server Configuration
      WEDGE_AUTH_SERVER_PORT: 9001
      JWT_ISSUER: https://staging-auth.example.com
      
      # Session Storage - Redis
      SESSION_STORAGE_TYPE: redis
      REDIS_HOST: redis
      REDIS_PORT: 6379
      REDIS_PASSWORD: staging-redis-password
      REDIS_USERNAME: default
      REDIS_DATABASE: 0
      
      # Session TTL Configuration
      AUTH_SESSION_TTL: 600        # 10 minutes for PKCE flow
      HTTP_SESSION_TTL: 1800       # 30 minutes for user sessions
      
      # Redis Namespaces
      AUTH_SESSION_NAMESPACE: wedge:staging:auth:session
      HTTP_SESSION_NAMESPACE: wedge:staging:http:session
      REDIS_TOKEN_NAMESPACE: wedge:staging:oauth2:auth
      
      # User Provider
      USER_PROVIDER_ENDPOINT: http://user-service:8081/api/users/validate
      
      # Logging
      LOGGING_LEVEL_ROOT: INFO
      LOGGING_LEVEL_APP: DEBUG

  redis:
    image: redis:7-alpine
    command: >
      redis-server
      --requirepass staging-redis-password
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data

volumes:
  redis-data:
```

### What's Different

- ✅ Redis for distributed sessions (supports multiple instances)
- ✅ Sessions persist across restarts
- ✅ Can scale horizontally
- ⚠️ Still uses test JWT keys (tokens invalidated on restart)

### Using Existing Redis

If you already have a Redis instance running, simply use `docker run`:

```bash
docker run -p 9001:9001 \
  -e SESSION_STORAGE_TYPE=redis \
  -e REDIS_HOST=your-redis-host.example.com \
  -e REDIS_PORT=6379 \
  -e REDIS_PASSWORD=your-redis-password \
  -e REDIS_USERNAME=default \
  -e AUTH_SESSION_TTL=600 \
  -e HTTP_SESSION_TTL=1800 \
  wedge-auth:jvm
```

### Scaling

Run multiple instances:
```bash
docker-compose up --scale wedge-auth=3
```

---

## Scenario 4: Database-Backed

**Perfect for:** Persistent client storage, production deployments, dynamic client management

**Can be used in:** Staging or production where clients need to persist across restarts

### Prerequisites

1. **PostgreSQL database** for client storage
2. **Redis** for session/token storage
3. **RSA key pair** for JWT signing (see [key_generation_rsa.md](../key_generation_rsa.md))

### Preparation

Generate RSA keys:
```bash
# Generate private key
openssl genrsa -out private-key.pem 2048

# Extract public key
openssl rsa -in private-key.pem -pubout -out public-key.pem
```

### Docker Compose

```yaml
version: '3.8'

services:
  wedge-auth:
    image: wedge-auth:jvm
    ports:
      - "9001:9001"
    depends_on:
      - postgres
      - redis
    volumes:
      # Mount JWT keys
      - ./keys/private-key.pem:/app/keys/private-key.pem:ro
      - ./keys/public-key.pem:/app/keys/public-key.pem:ro
    environment:
      # Server Configuration
      WEDGE_AUTH_SERVER_PORT: 9001
      JWT_ISSUER: https://auth.example.com
      
      # Client Storage - PostgreSQL
      CLIENT_STORAGE_TYPE: postgresql
      CLIENT_DB_URL: jdbc:postgresql://postgres:5432/oauth2_clients
      CLIENT_DB_USERNAME: wedge_user
      CLIENT_DB_PASSWORD: ${DB_PASSWORD}  # Use secrets management
      CLIENT_DB_DRIVER: org.postgresql.Driver
      CLIENT_DB_SCHEMA: public
      CLIENT_DB_AUTO_CREATE_SCHEMA: true
      
      # Connection Pool
      CLIENT_DB_POOL_SIZE: 10
      CLIENT_DB_MIN_IDLE: 5
      
      # Session Storage - Redis
      SESSION_STORAGE_TYPE: redis
      REDIS_HOST: redis
      REDIS_PORT: 6379
      REDIS_PASSWORD: ${REDIS_PASSWORD}  # Use secrets management
      REDIS_USERNAME: default
      REDIS_DATABASE: 0
      REDIS_SSL_ENABLED: false
      
      # Session TTL
      AUTH_SESSION_TTL: 600
      HTTP_SESSION_TTL: 3600  # 1 hour for production
      
      # JWT Configuration - File-based keys
      JWT_KEY_TYPE: file
      JWT_PRIVATE_KEY_PATH: /app/keys/private-key.pem
      JWT_PUBLIC_KEY_PATH: /app/keys/public-key.pem
      
      # Token Configuration
      IS_REFRESH_TOKEN_ENABLED: true
      ACCESS_TOKEN_TTL: 1800        # 30 minutes
      REFRESH_TOKEN_TTL: 2592000    # 30 days
      
      # User Provider
      USER_PROVIDER_ENDPOINT: https://api.example.com/users/validate
      USER_PROVIDER_TIMEOUT: 5000
      
      # Logging
      LOGGING_LEVEL_ROOT: WARN
      LOGGING_LEVEL_APP: INFO

  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: oauth2_clients
      POSTGRES_USER: wedge_user
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres-data:/var/lib/postgresql/data
    ports:
      - "5432:5432"

  redis:
    image: redis:7-alpine
    command: >
      redis-server
      --requirepass ${REDIS_PASSWORD}
    volumes:
      - redis-data:/data
    ports:
      - "6379:6379"

volumes:
  postgres-data:
  redis-data:
```

### Environment File (.env)

```bash
# Secrets (DO NOT COMMIT)
DB_PASSWORD=your-secure-db-password
REDIS_PASSWORD=your-secure-redis-password
```

### What's Different

- ✅ PostgreSQL for persistent client storage
- ✅ File-based JWT keys (tokens persist across restarts)
- ✅ Production-grade security
- ✅ Longer session TTLs

### Using Existing PostgreSQL and Redis

If you already have PostgreSQL and Redis running, use `docker run` with your existing infrastructure:

```bash
docker run -p 9001:9001 \
  -v /path/to/your/keys/private-key.pem:/app/keys/private-key.pem:ro \
  -v /path/to/your/keys/public-key.pem:/app/keys/public-key.pem:ro \
  -e CLIENT_STORAGE_TYPE=postgresql \
  -e CLIENT_DB_URL=jdbc:postgresql://your-db-host.example.com:5432/oauth2_clients \
  -e CLIENT_DB_USERNAME=wedge_user \
  -e CLIENT_DB_PASSWORD=your-secure-db-password \
  -e CLIENT_DB_AUTO_CREATE_SCHEMA=true \
  -e SESSION_STORAGE_TYPE=redis \
  -e REDIS_HOST=your-redis-host.example.com \
  -e REDIS_PORT=6379 \
  -e REDIS_PASSWORD=your-secure-redis-password \
  -e JWT_KEY_TYPE=file \
  -e JWT_PRIVATE_KEY_PATH=/app/keys/private-key.pem \
  -e JWT_PUBLIC_KEY_PATH=/app/keys/public-key.pem \
  -e JWT_ISSUER=https://auth.example.com \
  wedge-auth:jvm
```

### Database Management

Clients are stored in PostgreSQL. Use Flyway migrations (auto-applied on startup) or manage via database admin tools.

---

## Scenario 5: Full Stack

**Perfect for:** Custom branding, multi-language support, enterprise deployments

**Can be used in:** Any environment where you need custom UI (dev, staging, production)

### Prerequisites

Same as Database-Backed scenario, plus:
- **Custom UI templates** (Thymeleaf)
- **Custom static assets** (CSS, JS, images)
- **i18n message bundles** (optional)

### Directory Structure

```
deployment/
├── docker-compose.yml
├── .env
├── keys/
│   ├── private-key.pem
│   └── public-key.pem
├── custom-ui/
│   ├── templates/
│   │   ├── login.html
│   │   └── consent.html
│   ├── static/
│   │   ├── css/
│   │   ├── js/
│   │   └── images/
│   └── i18n/
│       ├── messages_en.properties
│       └── messages_es.properties
```

### Docker Compose

```yaml
version: '3.8'

services:
  wedge-auth:
    image: wedge-auth:jvm
    ports:
      - "9001:9001"
    depends_on:
      - postgres
      - redis
    volumes:
      # JWT Keys
      - ./keys/private-key.pem:/app/keys/private-key.pem:ro
      - ./keys/public-key.pem:/app/keys/public-key.pem:ro
      
      # Custom UI
      - ./custom-ui/templates:/app/custom-ui/templates:ro
      - ./custom-ui/static:/app/custom-ui/static:ro
      - ./custom-ui/i18n:/app/custom-ui/i18n:ro
      
    environment:
      # Server Configuration
      WEDGE_AUTH_SERVER_PORT: 9001
      JWT_ISSUER: https://auth.example.com
      
      # Client Storage - PostgreSQL
      CLIENT_STORAGE_TYPE: postgresql
      CLIENT_DB_URL: jdbc:postgresql://postgres:5432/oauth2_clients
      CLIENT_DB_USERNAME: wedge_user
      CLIENT_DB_PASSWORD: ${DB_PASSWORD}
      CLIENT_DB_DRIVER: org.postgresql.Driver
      CLIENT_DB_SCHEMA: public
      CLIENT_DB_AUTO_CREATE_SCHEMA: true
      CLIENT_DB_POOL_SIZE: 20
      CLIENT_DB_MIN_IDLE: 10
      
      # Session Storage - Redis
      SESSION_STORAGE_TYPE: redis
      REDIS_HOST: redis
      REDIS_PORT: 6379
      REDIS_PASSWORD: ${REDIS_PASSWORD}
      REDIS_USERNAME: default
      REDIS_DATABASE: 0
      REDIS_SSL_ENABLED: true  # Enable in production
      
      # Session TTL
      AUTH_SESSION_TTL: 600
      HTTP_SESSION_TTL: 3600
      
      # JWT Configuration
      JWT_KEY_TYPE: file
      JWT_PRIVATE_KEY_PATH: /app/keys/private-key.pem
      JWT_PUBLIC_KEY_PATH: /app/keys/public-key.pem
      
      # Token Configuration
      IS_REFRESH_TOKEN_ENABLED: true
      ACCESS_TOKEN_TTL: 1800
      REFRESH_TOKEN_TTL: 2592000
      
      # Custom Frontend
      FRONTEND_TEMPLATES_PATH: file:///app/custom-ui/templates
      FRONTEND_STATIC_PATH: file:///app/custom-ui/static
      FRONTEND_I18N_BASENAME: file:///app/custom-ui/i18n/messages
      FRONTEND_DEFAULT_LOCALE: en
      FRONTEND_SUPPORTED_LOCALES: en,es,fr
      
      # User Provider
      USER_PROVIDER_ENDPOINT: https://api.example.com/users/validate
      USER_PROVIDER_TIMEOUT: 5000
      
      # Virtual Threads (Java 21+)
      SPRING_THREADS_VIRTUAL_ENABLED: true
      
      # Logging
      LOGGING_LEVEL_ROOT: WARN
      LOGGING_LEVEL_APP: INFO
      LOGGING_LEVEL_SECURITY: WARN

  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: oauth2_clients
      POSTGRES_USER: wedge_user
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres-data:/var/lib/postgresql/data
    restart: unless-stopped

  redis:
    image: redis:7-alpine
    command: >
      redis-server
      --requirepass ${REDIS_PASSWORD}
      --tls-port 6380
      --port 0
    volumes:
      - redis-data:/data
    restart: unless-stopped

volumes:
  postgres-data:
  redis-data:
```

### What's Different

- ✅ Custom branded login/consent pages
- ✅ Multi-language support (i18n)
- ✅ TLS-enabled Redis
- ✅ Higher connection pool limits
- ✅ Production logging levels
- ✅ Virtual threads enabled for high concurrency

### Custom UI Setup

See [CUSTOM_LOGIN_TUTORIAL.md](./CUSTOM_LOGIN_TUTORIAL.md) for detailed instructions on creating custom templates.

---

## Environment Variables Reference

### Server Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| `WEDGE_AUTH_SERVER_PORT` | `9001` | HTTP port for the server |
| `SERVER_CONTEXT_PATH` | `/` | Application context path |
| `SPRING_APPLICATION_NAME` | `wedge-authorization-server` | Application name |
| `SPRING_THREADS_VIRTUAL_ENABLED` | `true` | Enable Java virtual threads |

### Client Storage

| Variable | Default | Description |
|----------|---------|-------------|
| `CLIENT_STORAGE_TYPE` | `none` | Storage type: `none`, `postgresql`, `mysql`, `sqlserver` |
| `CLIENT_DB_URL` | `jdbc:postgresql://localhost:5432/oauth2_clients` | JDBC URL |
| `CLIENT_DB_USERNAME` | `postgres` | Database username |
| `CLIENT_DB_PASSWORD` | `postgres` | Database password |
| `CLIENT_DB_DRIVER` | `org.postgresql.Driver` | JDBC driver class |
| `CLIENT_DB_SCHEMA` | `public` | Database schema name |
| `CLIENT_DB_AUTO_CREATE_SCHEMA` | `false` | Auto-create schema via Flyway |
| `CLIENT_DB_POOL_SIZE` | `10` | Max connection pool size |
| `CLIENT_DB_MIN_IDLE` | `5` | Min idle connections |

### Session Storage

| Variable | Default | Description |
|----------|---------|-------------|
| `SESSION_STORAGE_TYPE` | `in-memory` | Storage type: `in-memory` or `redis` |
| `AUTH_SESSION_TTL` | `600` | PKCE session TTL (seconds) |
| `HTTP_SESSION_TTL` | `1800` | User login session TTL (seconds) |
| `SESSION_MAX_SIZE` | `10000` | Max in-memory sessions |

### Redis Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| `REDIS_HOST` | `127.0.0.1` | Redis hostname |
| `REDIS_PORT` | `6379` | Redis port |
| `REDIS_PASSWORD` | `passwordtest` | Redis password |
| `REDIS_USERNAME` | `redistest` | Redis username |
| `REDIS_DATABASE` | `0` | Redis database index |
| `REDIS_SSL_ENABLED` | `false` | Enable TLS/SSL |
| `AUTH_SESSION_NAMESPACE` | `wedge:auth:session` | Key prefix for auth sessions |
| `HTTP_SESSION_NAMESPACE` | `wedge:http:session` | Key prefix for HTTP sessions |
| `REDIS_TOKEN_NAMESPACE` | `wedge:oauth2:auth` | Key prefix for tokens |

### JWT Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| `JWT_ISSUER` | `http://localhost:9001` | Issuer claim (iss) in tokens |
| `JWT_KEY_TYPE` | `test` | Key source: `test` or `file` |
| `JWT_KEY_SIZE` | `2048` | RSA key size (test mode only) |
| `JWT_PRIVATE_KEY_PATH` | `` | Path to private key PEM file |
| `JWT_PUBLIC_KEY_PATH` | `` | Path to public key PEM file |

### Token Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| `IS_REFRESH_TOKEN_ENABLED` | `true` | Enable refresh tokens |
| `ACCESS_TOKEN_TTL` | `1800` | Access token TTL (seconds) |
| `REFRESH_TOKEN_TTL` | `2592000` | Refresh token TTL (seconds) |
| `TOKEN_STORAGE_MAX_TTL` | `2592000` | Max token storage TTL |
| `TOKEN_STORAGE_MAX_SIZE` | `50000` | Max in-memory tokens |

### User Provider

| Variable | Default | Description |
|----------|---------|-------------|
| `DEFAULT_TENANT_ID` | `default-tenant` | Default tenant ID |
| `DEFAULT_TENANT_NAME` | `Default Tenant` | Default tenant name |
| `USER_PROVIDER_ENDPOINT` | `http://localhost:8081/api/users/validate` | User validation endpoint |
| `USER_PROVIDER_TIMEOUT` | `5000` | API timeout (milliseconds) |

### Frontend Customization

| Variable | Default | Description |
|----------|---------|-------------|
| `FRONTEND_TEMPLATES_PATH` | `classpath:templates` | Thymeleaf templates path |
| `FRONTEND_STATIC_PATH` | `classpath:static` | Static assets path |
| `FRONTEND_I18N_BASENAME` | `classpath:i18n/messages` | i18n message bundles |
| `FRONTEND_DEFAULT_LOCALE` | `en` | Default locale |
| `FRONTEND_SUPPORTED_LOCALES` | `en,es` | Supported locales (comma-separated) |

### OAuth Client Configuration (YAML mode only)

Each client can be customized via environment variables:

**Public Client:**
- `PUBLIC_CLIENT_ID`
- `PUBLIC_CLIENT_NAME`
- `PUBLIC_CLIENT_AUTH_METHODS`
- `PUBLIC_CLIENT_GRANT_TYPES`
- `PUBLIC_CLIENT_REDIRECT_URIS`
- `PUBLIC_CLIENT_POST_LOGOUT_URIS`
- `PUBLIC_CLIENT_SCOPES`
- `PUBLIC_CLIENT_REQUIRE_CONSENT`
- `PUBLIC_CLIENT_REQUIRE_PKCE`
- `PUBLIC_CLIENT_TENANT_ID`

**Confidential Client:**
- `CONFIDENTIAL_CLIENT_ID`
- `CONFIDENTIAL_CLIENT_SECRET`
- `CONFIDENTIAL_CLIENT_NAME`
- `CONFIDENTIAL_CLIENT_AUTH_METHODS`
- `CONFIDENTIAL_CLIENT_GRANT_TYPES`
- `CONFIDENTIAL_CLIENT_REDIRECT_URIS`
- `CONFIDENTIAL_CLIENT_SCOPES`
- `CONFIDENTIAL_CLIENT_REQUIRE_CONSENT`
- `CONFIDENTIAL_CLIENT_REQUIRE_PKCE`
- `CONFIDENTIAL_CLIENT_TENANT_ID`

### Logging

| Variable | Default | Description |
|----------|---------|-------------|
| `LOGGING_LEVEL_ROOT` | `INFO` | Root logger level |
| `LOGGING_LEVEL_APP` | `DEBUG` | Application logger level |
| `LOGGING_LEVEL_SECURITY` | `DEBUG` | Spring Security logger |
| `LOGGING_LEVEL_OAUTH2` | `TRACE` | OAuth2 logger |

---

## Troubleshooting

### Sessions Not Persisting

**Problem:** Sessions lost on restart

**Solution:** 
- Verify `SESSION_STORAGE_TYPE=redis`
- Check Redis connection: `docker exec -it <redis-container> redis-cli ping`
- Verify Redis credentials

### JWT Tokens Invalid After Restart

**Problem:** Tokens become invalid when server restarts

**Solution:**
- Use `JWT_KEY_TYPE=file` with persistent RSA keys
- Mount key files as volumes in Docker
- Never use `JWT_KEY_TYPE=test` in production

### Database Connection Errors

**Problem:** Cannot connect to PostgreSQL

**Solution:**
```bash
# Test database connection
docker exec -it <postgres-container> psql -U wedge_user -d oauth2_clients

# Check logs
docker logs <wedge-auth-container>

# Verify environment variables
docker exec <wedge-auth-container> env | grep CLIENT_DB
```

### Custom UI Not Loading

**Problem:** Custom templates not found

**Solution:**
- Verify volume mounts: `docker inspect <container> | grep Mounts`
- Check file permissions (must be readable by container user)
- Use absolute paths with `file://` prefix
- Check logs for template resolution errors

### Redis Connection Timeout

**Problem:** Cannot connect to Redis

**Solution:**
```bash
# Test Redis connection
docker exec -it <redis-container> redis-cli -a <password> ping

# Check network connectivity
docker network inspect <network-name>

# Verify Redis is accepting connections
docker logs <redis-container>
```

### High Memory Usage

**Problem:** Application consuming too much memory

**Solution:**
- Reduce `SESSION_MAX_SIZE` and `TOKEN_STORAGE_MAX_SIZE`
- Switch to Redis for session/token storage
- Adjust JVM memory settings in Dockerfile:
  ```dockerfile
  ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"
  ```

---

## Next Steps

- Review [oauth2-pkce-testing-guide.md](../oauth2-pkce-testing-guide.md) for testing OAuth flows
- See [CUSTOM_LOGIN_TUTORIAL.md](./CUSTOM_LOGIN_TUTORIAL.md) for UI customization
- Check [key_generation_rsa.md](../key_generation_rsa.md) for JWT key generation

For production deployments, consider:
- Load balancer configuration
- TLS/SSL certificates
- Monitoring and alerting
- Backup strategies for PostgreSQL and Redis
- Secrets management (HashiCorp Vault, AWS Secrets Manager, etc.)
