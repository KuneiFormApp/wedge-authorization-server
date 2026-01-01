# WedgeAuth Environment Variables Reference

Complete reference of all environment variables for configuring WedgeAuth.

## Quick Navigation

- [Server Configuration](#server-configuration)
- [Client Storage](#client-storage)
- [Session Storage](#session-storage)
- [Redis Configuration](#redis-configuration)
- [JWT Configuration](#jwt-configuration)
- [Token Configuration](#token-configuration)
- [User Provider](#user-provider)
- [Frontend Customization](#frontend-customization)
- [OAuth Client Configuration](#oauth-client-configuration)
- [Logging](#logging)
- [Management Endpoints](#management-endpoints)

---

## Server Configuration

| Variable | Type | Default | Description |
|----------|------|---------|-------------|
| `WEDGE_AUTH_SERVER_PORT` | Integer | `9001` | HTTP port for the authorization server |
| `SERVER_CONTEXT_PATH` | String | `/` | Application context path (e.g., `/auth`) |
| `SPRING_APPLICATION_NAME` | String | `wedge-authorization-server` | Application name used in logs and metrics |
| `SPRING_THREADS_VIRTUAL_ENABLED` | Boolean | `true` | Enable Java 21+ virtual threads for high concurrency |

**Example:**
```bash
WEDGE_AUTH_SERVER_PORT=8080
SERVER_CONTEXT_PATH=/auth
SPRING_THREADS_VIRTUAL_ENABLED=true
```

---

## Client Storage

Controls where OAuth2 client configurations are stored.

| Variable | Type | Default | Description |
|----------|------|---------|-------------|
| `CLIENT_STORAGE_TYPE` | Enum | `none` | Storage type: `none` (YAML), `postgresql`, `mysql`, `sqlserver` |
| `CLIENT_DB_URL` | String | `jdbc:postgresql://localhost:5432/oauth2_clients` | JDBC connection URL |
| `CLIENT_DB_USERNAME` | String | `postgres` | Database username |
| `CLIENT_DB_PASSWORD` | String | `postgres` | Database password ⚠️ **Use secrets** |
| `CLIENT_DB_DRIVER` | String | `org.postgresql.Driver` | JDBC driver class name |
| `CLIENT_DB_SCHEMA` | String | `public` | Database schema name |
| `CLIENT_DB_AUTO_CREATE_SCHEMA` | Boolean | `false` | Auto-create schema using Flyway migrations |
| `CLIENT_DB_POOL_SIZE` | Integer | `10` | Maximum connection pool size (HikariCP) |
| `CLIENT_DB_MIN_IDLE` | Integer | `5` | Minimum idle connections in pool |
| `CLIENT_DB_CONNECTION_TIMEOUT` | Integer | `30000` | Connection timeout in milliseconds |
| `CLIENT_DB_IDLE_TIMEOUT` | Integer | `600000` | Idle connection timeout (10 minutes) |
| `CLIENT_DB_MAX_LIFETIME` | Integer | `1800000` | Maximum connection lifetime (30 minutes) |
| `CLIENT_DB_AUTO_COMMIT` | Boolean | `true` | Enable auto-commit for connections |

**PostgreSQL Example:**
```bash
CLIENT_STORAGE_TYPE=postgresql
CLIENT_DB_URL=jdbc:postgresql://db.example.com:5432/oauth2_clients
CLIENT_DB_USERNAME=wedge_user
CLIENT_DB_PASSWORD=secure-password
CLIENT_DB_DRIVER=org.postgresql.Driver
CLIENT_DB_SCHEMA=public
CLIENT_DB_AUTO_CREATE_SCHEMA=true
CLIENT_DB_POOL_SIZE=20
```

**MySQL Example:**
```bash
CLIENT_STORAGE_TYPE=mysql
CLIENT_DB_URL=jdbc:mysql://db.example.com:3306/oauth2_clients
CLIENT_DB_USERNAME=wedge_user
CLIENT_DB_PASSWORD=secure-password
CLIENT_DB_DRIVER=com.mysql.cj.jdbc.Driver
```

**SQL Server Example:**
```bash
CLIENT_STORAGE_TYPE=sqlserver
CLIENT_DB_URL=jdbc:sqlserver://db.example.com:1433;databaseName=oauth2_clients
CLIENT_DB_USERNAME=wedge_user
CLIENT_DB_PASSWORD=secure-password
CLIENT_DB_DRIVER=com.microsoft.sqlserver.jdbc.SQLServerDriver
```

---

## Session Storage

Controls where authentication sessions are stored.

| Variable | Type | Default | Description |
|----------|------|---------|-------------|
| `SESSION_STORAGE_TYPE` | Enum | `in-memory` | Storage type: `in-memory` or `redis` |
| `AUTH_SESSION_TTL` | Integer | `600` | PKCE authorization session TTL in seconds (10 min) |
| `HTTP_SESSION_TTL` | Integer | `1800` | User login session TTL in seconds (30 min) |
| `SESSION_MAX_SIZE` | Integer | `10000` | Maximum in-memory sessions (ignored if Redis) |

**In-Memory Example:**
```bash
SESSION_STORAGE_TYPE=in-memory
AUTH_SESSION_TTL=600
HTTP_SESSION_TTL=1800
SESSION_MAX_SIZE=10000
```

**Redis Example:**
```bash
SESSION_STORAGE_TYPE=redis
AUTH_SESSION_TTL=600
HTTP_SESSION_TTL=3600
```

---

## Redis Configuration

Used for distributed session and token storage.

| Variable | Type | Default | Description |
|----------|------|---------|-------------|
| `REDIS_HOST` | String | `127.0.0.1` | Redis server hostname or IP |
| `REDIS_PORT` | Integer | `6379` | Redis server port |
| `REDIS_PASSWORD` | String | `passwordtest` | Redis password ⚠️ **Use secrets** |
| `REDIS_USERNAME` | String | `redistest` | Redis username (Redis 6+) |
| `REDIS_DATABASE` | Integer | `0` | Redis database index (0-15) |
| `REDIS_SSL_ENABLED` | Boolean | `false` | Enable TLS/SSL for Redis connections |
| `AUTH_SESSION_NAMESPACE` | String | `wedge:auth:session` | Key prefix for PKCE sessions |
| `HTTP_SESSION_NAMESPACE` | String | `wedge:http:session` | Key prefix for HTTP sessions |
| `REDIS_TOKEN_NAMESPACE` | String | `wedge:oauth2:auth` | Key prefix for OAuth2 tokens |

**Development Example:**
```bash
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=dev-password
REDIS_USERNAME=default
REDIS_DATABASE=0
REDIS_SSL_ENABLED=false
AUTH_SESSION_NAMESPACE=wedge:dev:auth:session
HTTP_SESSION_NAMESPACE=wedge:dev:http:session
REDIS_TOKEN_NAMESPACE=wedge:dev:oauth2:auth
```

**Production Example:**
```bash
REDIS_HOST=redis.example.com
REDIS_PORT=6380
REDIS_PASSWORD=prod-secure-password
REDIS_USERNAME=wedge_redis
REDIS_DATABASE=0
REDIS_SSL_ENABLED=true
AUTH_SESSION_NAMESPACE=wedge:prod:auth:session
HTTP_SESSION_NAMESPACE=wedge:prod:http:session
REDIS_TOKEN_NAMESPACE=wedge:prod:oauth2:auth
```

---

## JWT Configuration

Controls JWT token signing and validation.

| Variable | Type | Default | Description |
|----------|------|---------|-------------|
| `JWT_ISSUER` | String | `http://localhost:9001` | Issuer claim (`iss`) in JWT tokens |
| `JWT_KEY_TYPE` | Enum | `test` | Key source: `test` (generated) or `file` (from disk) |
| `JWT_KEY_SIZE` | Integer | `2048` | RSA key size in bits (test mode only) |
| `JWT_PRIVATE_KEY_PATH` | String | `` | Path to private key PEM file (file mode) |
| `JWT_PUBLIC_KEY_PATH` | String | `` | Path to public key PEM file (file mode) |

**Test Mode (Development):**
```bash
JWT_ISSUER=http://localhost:9001
JWT_KEY_TYPE=test
JWT_KEY_SIZE=2048
```

**File Mode (Production):**
```bash
JWT_ISSUER=https://auth.example.com
JWT_KEY_TYPE=file
JWT_PRIVATE_KEY_PATH=/app/keys/private-key.pem
JWT_PUBLIC_KEY_PATH=/app/keys/public-key.pem
```

> ⚠️ **Important:** Never use `JWT_KEY_TYPE=test` in production! Keys are regenerated on restart, invalidating all existing tokens.

---

## Token Configuration

Controls OAuth2 token behavior and storage.

| Variable | Type | Default | Description |
|----------|------|---------|-------------|
| `IS_REFRESH_TOKEN_ENABLED` | Boolean | `true` | Enable refresh token issuance |
| `ACCESS_TOKEN_TTL` | Integer | `1800` | Access token TTL in seconds (30 min) |
| `REFRESH_TOKEN_TTL` | Integer | `2592000` | Refresh token TTL in seconds (30 days) |
| `TOKEN_STORAGE_MAX_TTL` | Integer | `2592000` | Maximum token storage TTL (30 days) |
| `TOKEN_STORAGE_MAX_SIZE` | Integer | `50000` | Maximum in-memory tokens (Caffeine cache) |

**Example:**
```bash
IS_REFRESH_TOKEN_ENABLED=true
ACCESS_TOKEN_TTL=1800      # 30 minutes
REFRESH_TOKEN_TTL=2592000  # 30 days
TOKEN_STORAGE_MAX_SIZE=50000
```

**Short-lived tokens (high security):**
```bash
ACCESS_TOKEN_TTL=300       # 5 minutes
REFRESH_TOKEN_TTL=86400    # 1 day
```

**Long-lived tokens (convenience):**
```bash
ACCESS_TOKEN_TTL=3600      # 1 hour
REFRESH_TOKEN_TTL=7776000  # 90 days
```

---

## User Provider

Configuration for external user authentication service.

| Variable | Type | Default | Description |
|----------|------|---------|-------------|
| `DEFAULT_TENANT_ID` | String | `default-tenant` | Default tenant identifier |
| `DEFAULT_TENANT_NAME` | String | `Default Tenant` | Default tenant display name |
| `USER_PROVIDER_ENDPOINT` | String | `http://localhost:8081/api/users/validate` | User validation API endpoint |
| `USER_PROVIDER_TIMEOUT` | Integer | `5000` | API call timeout in milliseconds |

**Example:**
```bash
DEFAULT_TENANT_ID=acme-corp
DEFAULT_TENANT_NAME=ACME Corporation
USER_PROVIDER_ENDPOINT=https://api.acme.com/auth/validate
USER_PROVIDER_TIMEOUT=3000
```

---

## Frontend Customization

Controls custom UI templates and localization.

| Variable | Type | Default | Description |
|----------|------|---------|-------------|
| `FRONTEND_TEMPLATES_PATH` | String | `classpath:templates` | Path to Thymeleaf templates |
| `FRONTEND_STATIC_PATH` | String | `classpath:static` | Path to static assets (CSS/JS/images) |
| `FRONTEND_I18N_BASENAME` | String | `classpath:i18n/messages` | Path to i18n message bundles |
| `FRONTEND_DEFAULT_LOCALE` | String | `en` | Default locale if not specified by browser |
| `FRONTEND_SUPPORTED_LOCALES` | String | `en,es` | Comma-separated list of supported locales |

**Default (Built-in UI):**
```bash
FRONTEND_TEMPLATES_PATH=classpath:templates
FRONTEND_STATIC_PATH=classpath:static
FRONTEND_I18N_BASENAME=classpath:i18n/messages
FRONTEND_DEFAULT_LOCALE=en
FRONTEND_SUPPORTED_LOCALES=en,es
```

**Custom UI (File System):**
```bash
FRONTEND_TEMPLATES_PATH=file:///opt/wedge/custom-ui/templates
FRONTEND_STATIC_PATH=file:///opt/wedge/custom-ui/static
FRONTEND_I18N_BASENAME=file:///opt/wedge/custom-ui/i18n/messages
FRONTEND_DEFAULT_LOCALE=en
FRONTEND_SUPPORTED_LOCALES=en,es,fr,de
```

> 📝 **Note:** See [CUSTOM_LOGIN_TUTORIAL.md](./CUSTOM_LOGIN_TUTORIAL.md) for creating custom templates.

---

## OAuth Client Configuration

Configure default OAuth2 clients (only used when `CLIENT_STORAGE_TYPE=none`).

### Public SPA Client

| Variable | Default | Description |
|----------|---------|-------------|
| `PUBLIC_CLIENT_ID` | `public-spa-client` | Client identifier |
| `PUBLIC_CLIENT_NAME` | `Public SPA Client` | Client display name |
| `PUBLIC_CLIENT_AUTH_METHODS` | `none` | Authentication methods (comma-separated) |
| `PUBLIC_CLIENT_GRANT_TYPES` | `authorization_code,refresh_token` | Allowed grant types |
| `PUBLIC_CLIENT_REDIRECT_URIS` | `http://localhost:3000/callback,...` | Redirect URIs (comma-separated) |
| `PUBLIC_CLIENT_POST_LOGOUT_URIS` | `http://localhost:3000/` | Post-logout URIs (comma-separated) |
| `PUBLIC_CLIENT_SCOPES` | `openid,profile,email,read,write,offline_access` | Allowed scopes |
| `PUBLIC_CLIENT_REQUIRE_CONSENT` | `false` | Require user consent screen |
| `PUBLIC_CLIENT_REQUIRE_PKCE` | `true` | Require PKCE for authorization code flow |
| `PUBLIC_CLIENT_TENANT_ID` | `default-tenant` | Associated tenant ID |

**Example:**
```bash
PUBLIC_CLIENT_ID=my-spa-app
PUBLIC_CLIENT_NAME=My SPA Application
PUBLIC_CLIENT_REDIRECT_URIS=https://app.example.com/callback,https://app.example.com/silent-renew
PUBLIC_CLIENT_POST_LOGOUT_URIS=https://app.example.com/
PUBLIC_CLIENT_SCOPES=openid,profile,email,read,write
PUBLIC_CLIENT_REQUIRE_PKCE=true
```

### Confidential Backend Client

| Variable | Default | Description |
|----------|---------|-------------|
| `CONFIDENTIAL_CLIENT_ID` | `confidential-backend-client` | Client identifier |
| `CONFIDENTIAL_CLIENT_SECRET` | `secret` | Client secret ⚠️ **Use secrets** |
| `CONFIDENTIAL_CLIENT_NAME` | `Confidential Backend Client` | Client display name |
| `CONFIDENTIAL_CLIENT_AUTH_METHODS` | `client_secret_basic,client_secret_post` | Authentication methods |
| `CONFIDENTIAL_CLIENT_GRANT_TYPES` | `client_credentials,authorization_code,refresh_token` | Allowed grant types |
| `CONFIDENTIAL_CLIENT_REDIRECT_URIS` | `http://localhost:8082/callback` | Redirect URIs |
| `CONFIDENTIAL_CLIENT_SCOPES` | `openid,profile,email,admin,offline_access` | Allowed scopes |
| `CONFIDENTIAL_CLIENT_REQUIRE_CONSENT` | `false` | Require user consent |
| `CONFIDENTIAL_CLIENT_REQUIRE_PKCE` | `true` | Require PKCE |
| `CONFIDENTIAL_CLIENT_TENANT_ID` | `default-tenant` | Associated tenant ID |

**Example:**
```bash
CONFIDENTIAL_CLIENT_ID=backend-service
CONFIDENTIAL_CLIENT_SECRET=super-secure-secret-key
CONFIDENTIAL_CLIENT_NAME=Backend API Service
CONFIDENTIAL_CLIENT_GRANT_TYPES=client_credentials,authorization_code
CONFIDENTIAL_CLIENT_SCOPES=openid,admin,api:read,api:write
```

---

## Logging

Control log levels for different components.

| Variable | Default | Description |
|----------|---------|-------------|
| `LOGGING_LEVEL_ROOT` | `INFO` | Root logger level |
| `LOGGING_LEVEL_APP` | `DEBUG` | Application logger (`com.kuneiform`) |
| `LOGGING_LEVEL_TOKEN_GEN` | `DEBUG` | Token generation logger |
| `LOGGING_LEVEL_OAUTH2_TOKEN` | `DEBUG` | OAuth2 token internals |
| `LOGGING_LEVEL_SECURITY` | `DEBUG` | Spring Security logger |
| `LOGGING_LEVEL_SECURITY_WEB` | `TRACE` | Security filter chain |
| `LOGGING_LEVEL_OAUTH2` | `TRACE` | OAuth2 internals |

**Valid Levels:** `TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR`, `OFF`

**Development:**
```bash
LOGGING_LEVEL_ROOT=DEBUG
LOGGING_LEVEL_APP=TRACE
LOGGING_LEVEL_SECURITY=DEBUG
LOGGING_LEVEL_OAUTH2=TRACE
```

**Production:**
```bash
LOGGING_LEVEL_ROOT=WARN
LOGGING_LEVEL_APP=INFO
LOGGING_LEVEL_SECURITY=WARN
LOGGING_LEVEL_OAUTH2=WARN
```

**Minimal (Performance):**
```bash
LOGGING_LEVEL_ROOT=ERROR
LOGGING_LEVEL_APP=WARN
LOGGING_LEVEL_SECURITY=ERROR
LOGGING_LEVEL_OAUTH2=ERROR
```

---

## Management Endpoints

Configure Spring Boot Actuator endpoints.

| Variable | Default | Description |
|----------|---------|-------------|
| `MANAGEMENT_ENDPOINTS_INCLUDE` | `*` | Exposed endpoints (comma-separated or `*`) |

**All Endpoints (Development):**
```bash
MANAGEMENT_ENDPOINTS_INCLUDE=*
```

**Limited Endpoints (Production):**
```bash
MANAGEMENT_ENDPOINTS_INCLUDE=health,info,metrics
```

**Available Endpoints:**
- `health` - Application health status
- `info` - Application information
- `metrics` - Application metrics
- `env` - Environment properties
- `loggers` - Logger configuration
- `threaddump` - Thread dump
- `heapdump` - Heap dump

---

## Complete Examples

### Minimal Development Setup

```bash
# Server
WEDGE_AUTH_SERVER_PORT=9001

# Logging
LOGGING_LEVEL_ROOT=INFO
LOGGING_LEVEL_APP=DEBUG
```

### Full Production Setup

```bash
# Server
WEDGE_AUTH_SERVER_PORT=9001
SERVER_CONTEXT_PATH=/
SPRING_THREADS_VIRTUAL_ENABLED=true

# Client Storage - PostgreSQL
CLIENT_STORAGE_TYPE=postgresql
CLIENT_DB_URL=jdbc:postgresql://db.example.com:5432/oauth2_clients
CLIENT_DB_USERNAME=wedge_user
CLIENT_DB_PASSWORD=${DB_PASSWORD}
CLIENT_DB_SCHEMA=public
CLIENT_DB_AUTO_CREATE_SCHEMA=true
CLIENT_DB_POOL_SIZE=20
CLIENT_DB_MIN_IDLE=10

# Session Storage - Redis
SESSION_STORAGE_TYPE=redis
REDIS_HOST=redis.example.com
REDIS_PORT=6379
REDIS_PASSWORD=${REDIS_PASSWORD}
REDIS_USERNAME=wedge_redis
REDIS_DATABASE=0
REDIS_SSL_ENABLED=true
AUTH_SESSION_TTL=600
HTTP_SESSION_TTL=3600

# JWT - File-based
JWT_ISSUER=https://auth.example.com
JWT_KEY_TYPE=file
JWT_PRIVATE_KEY_PATH=/app/keys/private-key.pem
JWT_PUBLIC_KEY_PATH=/app/keys/public-key.pem

# Tokens
IS_REFRESH_TOKEN_ENABLED=true
ACCESS_TOKEN_TTL=1800
REFRESH_TOKEN_TTL=2592000

# User Provider
USER_PROVIDER_ENDPOINT=https://api.example.com/users/validate
USER_PROVIDER_TIMEOUT=5000

# Custom UI
FRONTEND_TEMPLATES_PATH=file:///app/custom-ui/templates
FRONTEND_STATIC_PATH=file:///app/custom-ui/static
FRONTEND_I18N_BASENAME=file:///app/custom-ui/i18n/messages
FRONTEND_DEFAULT_LOCALE=en
FRONTEND_SUPPORTED_LOCALES=en,es,fr

# Logging
LOGGING_LEVEL_ROOT=WARN
LOGGING_LEVEL_APP=INFO

# Management
MANAGEMENT_ENDPOINTS_INCLUDE=health,info,metrics
```

---

## Security Best Practices

### ⚠️ Never Hardcode Secrets

Use environment files, secret managers, or orchestration tools:

**Docker Compose with .env:**
```yaml
environment:
  CLIENT_DB_PASSWORD: ${DB_PASSWORD}
  REDIS_PASSWORD: ${REDIS_PASSWORD}
```

**Kubernetes Secrets:**
```yaml
env:
  - name: CLIENT_DB_PASSWORD
    valueFrom:
      secretKeyRef:
        name: wedge-secrets
        key: db-password
```

**AWS Secrets Manager / HashiCorp Vault:**
Use init containers or sidecar patterns to inject secrets.

### 🔒 Production Checklist

- [ ] Use `JWT_KEY_TYPE=file` with persistent keys
- [ ] Use strong passwords for databases and Redis
- [ ] Enable `REDIS_SSL_ENABLED=true`
- [ ] Set appropriate token TTLs
- [ ] Use `LOGGING_LEVEL_ROOT=WARN` or higher
- [ ] Limit `MANAGEMENT_ENDPOINTS_INCLUDE`
- [ ] Use secrets management (not .env files)
- [ ] Enable database connection pooling
- [ ] Configure proper session TTLs

---

## Related Documentation

- [Deployment Guide](./deployment-guide.md) - Deployment scenarios
- [Quick Start](./quick-start.md) - Get started quickly
- [Docker Compose Examples](./docker-compose-examples.md) - Ready-to-use configurations
