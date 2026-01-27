# WedgeAuth Deployment Guide

This guide covers different deployment scenarios for the WedgeAuth Authorization Server, from minimal development setups to full production configurations.

## Table of Contents

1. [Deployment Scenarios](#deployment-scenarios)
2. [Full Stack Deployment (Recommended)](#full-stack-deployment-recommended)
    - [Example 1: Default Client Redirect](#example-1-default-client-redirect)
    - [Example 2: Account Page Redirect](#example-2-account-page-redirect)
    - [Example 3: No Application Redirect](#example-3-no-application-redirect)
3. [Important Configuration Notes](#important-configuration-notes)
    - [Redirect Modes](#redirect-modes)
    - [Secrets Management](#secrets-management)
    - [Docker Run vs Compose](#docker-run-vs-compose)
    - [Custom UI & Localization](#custom-ui--localization)
    - [API Key Configuration](#api-key-configuration)
    - [Circuit Breaker Configuration](#circuit-breaker-configuration)
4. [Alternative Configurations](#alternative-configurations)
    - [YAML-Only Configuration](#yaml-only-configuration)
    - [Multi-Tenant Configuration (Experimental)](#multi-tenant-configuration-experimental)
5. [Database Reference](#database-reference)
    - [Supported Databases](#supported-databases)
    - [SQL Scripts](#sql-scripts)
    - [Table Descriptions](#table-descriptions)
6. [API Contracts](#api-contracts)
    - [User Provider API](#user-provider-api)
    - [MFA Logic](#mfa-logic)

---

## Deployment Scenarios

WedgeAuth supports flexible deployment options:

- **Docker Compose**: Recommended for clarity and reproducible deployments.
- **Docker Run**: Simple single-command execution.
- **Kubernetes**: (Coming soon) for orchestrated environments.

---

## Full Stack Deployment (Recommended)

This is the production-ready configuration using:
- **PostgreSQL** (or MySQL/SQL Server) for client and tenant storage.
- **Redis** for distributed sessions and tokens.
- **File-based JWT Keys** for security.

### Example 1: Default Client Redirect

In this mode (`DEFAULT_REDIRECT_MODE=DEFAULT_CLIENT`), when a user accesses the root `/` or successfully logs in without a specific client request, they are redirected to a default client's application (e.g., a dashboard or landing page).

We are seting up some custom pages. By default, WedgeAuth has his own pages. But since is just HTML/CSS/JS you can import your own logins too.
For more information, see the [Custom UI & Localization](#custom-ui--localization)

**docker-compose.yml**:

```yaml
version: '3.8'

services:
  wedge-auth:
    image: ariandel007/wedge-authorization-server:0.0.1
    deploy:
      resources:
        limits:
          cpus: '0.5'   # 500m (500 millicores) = 0.5 CPU
          memory: 512M  # 700 MiB
        reservations:
          cpus: '0.2'   # 200m (200 millicores) = 0.2 CPU
          memory: 200M  # 200 MiB
    ports:
      - "9001:9001"
    volumes:
      # Mount JWT keys
      - E:/wedgeauth/private-key.pem:/app/keys/private-key.pem:ro
      - E:/wedgeauth/public-key.pem:/app/keys/public-key.pem:ro
      # Custom UI
      - E:/wedgeauth/wedge-custom-login/examples/01-minimal-clean/templates:/app/custom-ui/templates:ro
      - E:/wedgeauth/wedge-custom-login/examples/01-minimal-clean/static:/app/custom-ui/static:ro
      - E:/wedgeauth/wedge-custom-login/examples/01-minimal-clean/i18n:/app/custom-ui/i18n:ro

    environment:
      # Server Configuration
      WEDGE_AUTH_SERVER_PORT: 9001
      JWT_ISSUER: http://[IP_OR_DNS]:9001
      
      # Client Storage - PostgreSQL
      CLIENT_STORAGE_TYPE: "postgresql"
      CLIENT_DB_URL: "jdbc:postgresql://[IP_OR_DNS]:5432/kuneiform_bd_dev"
      CLIENT_DB_USERNAME: "postgresql_dev_user"
      CLIENT_DB_PASSWORD: "your_secure_password" # Use env vars in production
      CLIENT_DB_DRIVER: "org.postgresql.Driver"
      CLIENT_DB_SCHEMA: "kms"
      CLIENT_DB_AUTO_CREATE_SCHEMA: true
      
      # Connection Pool
      CLIENT_DB_POOL_SIZE: 10
      CLIENT_DB_MIN_IDLE: 5
      
      # Session Storage - Redis
      SESSION_STORAGE_TYPE: "redis"
      REDIS_HOST: "[IP_OR_DNS]"
      REDIS_PORT: "6379"
      REDIS_PASSWORD: "your_redis_password"
      REDIS_USERNAME: "redistest"
      REDIS_DATABASE: "0"
      REDIS_SSL_ENABLED: false
      
      # Session Idle Timeouts (reset on each request)
      AUTH_SESSION_TTL: 600
      HTTP_SESSION_TIMEOUT: 3600  # 1 hour idle timeout
      
      # JWT Configuration - File-based keys
      JWT_KEY_TYPE: "file"
      JWT_PRIVATE_KEY_PATH: "/app/keys/private-key.pem"
      JWT_PUBLIC_KEY_PATH: "/app/keys/public-key.pem"
      JWT_KEY_ID: "wedgeauth-key-1"
      
      # Token Configuration
      IS_REFRESH_TOKEN_ENABLED: true
      ACCESS_TOKEN_TTL: 180        # 3 minutes
      REFRESH_TOKEN_TTL: 2592000    # 30 days
            
      # Logging
      LOGGING_LEVEL_ROOT: INFO
      LOGGING_LEVEL_APP: INFO

      BPL_JVM_THREAD_COUNT : 50

      # TEMPLATES
      FRONTEND_TEMPLATES_PATH: file:///app/custom-ui/templates
      FRONTEND_STATIC_PATH: file:///app/custom-ui/static
      FRONTEND_I18N_BASENAME: file:///app/custom-ui/i18n/messages
      FRONTEND_DEFAULT_LOCALE: en
      FRONTEND_SUPPORTED_LOCALES: en,es

      # POST-LOGINS
      DEFAULT_REDIRECT_MODE: DEFAULT_CLIENT
      DEFAULT_LOGIN_CLIENT_ID: public-local-spa-client
      DEFAULT_LOGIN_REDIRECT_URI: http://[IP_OR_DNS]:3000/
      
      # API KEY CONFIGURATION
      API_KEY_MUST_BE_VALIDATED: true
      CUSTOM_API_KEY_HEADER: X-API-Key
      CUSTOM_API_KEY: your-secure-api-key

      # CIRCUIT BREAKER CONFIGURATION
      CIRCUIT_BREAKER_ENABLED: true
      CIRCUIT_BREAKER_FAILURE_RATE_THRESHOLD: 50
      CIRCUIT_BREAKER_MIN_CALLS: 10
      CIRCUIT_BREAKER_HALF_OPEN_CALLS: 5
      CIRCUIT_BREAKER_WAIT_DURATION_MS: 60000
      CIRCUIT_BREAKER_SLIDING_WINDOW_MS: 10000
      CIRCUIT_BREAKER_WINDOW_TYPE: count_based
      CIRCUIT_BREAKER_AUTO_TRANSITION: true
      
      #TENANCY
      DEFAULT_TENANT_ID: default
```

### Example 2: Account Page Redirect

In this mode (`DEFAULT_REDIRECT_MODE=ACCOUNT_PAGE`), users are redirected to an internal account management page hosted by WedgeAuth (or a configured URL) after login.

```yaml
    environment:
      # ... other config identical to Example 1 ...
      
      # POST-LOGINS
      DEFAULT_REDIRECT_MODE: ACCOUNT_PAGE
      # DEFAULT_LOGIN_CLIENT_ID and URI are ignored in this mode
```

### Example 3: No Application Redirect

In this mode (`DEFAULT_REDIRECT_MODE=NO_APPLICATION`), the user remains on a "Login Successful" page or the root page after authentication, without being automatically redirected to any client application. This is useful for API-only scenarios or when the user manually navigates.

```yaml
    environment:
      # ... other config identical to Example 1 ...
      
      # POST-LOGINS
      DEFAULT_REDIRECT_MODE: NO_APPLICATION
```

---

## Important Configuration Notes

### Redirect Modes

The `DEFAULT_REDIRECT_MODE` variable controls the behavior when a user accesses the root URL or completes a login flow initiated without a specific OAuth2 client context:

| Mode | Description |
|------|-------------|
| `DEFAULT_CLIENT` | Redirects the user to a specific specific client application. Requires `DEFAULT_LOGIN_CLIENT_ID` and `DEFAULT_LOGIN_REDIRECT_URI` to be set. |
| `ACCOUNT_PAGE` | Redirects the user to the generic account/profile management page of the Authorization Server. |
| `NO_APPLICATION` | Does not redirect. Shows a generic "Welcome" or status page. |

### Secrets Management

While the examples above show passwords directly in the `environment` block for clarity, **you should always use Environment Variables** for sensitive data in production.

Example with `.env` file:

```yaml
    environment:
      CLIENT_DB_PASSWORD: ${DB_PASSWORD}
      REDIS_PASSWORD: ${REDIS_PASSWORD}
```

### Docker Run vs Compose

The Docker Compose examples are provided for clarity and organization. However, you can easily run WedgeAuth using a standard `docker run` command:

```bash
docker run -d \
  --name wedge-auth \
  -p 9001:9001 \
  -v /path/to/keys:/app/keys:ro \
  -e CLIENT_DB_PASSWORD=secret \
  -e ... (other env vars) \
  ariandel007/wedge-authorization-server:0.0.1
```

You can connect to **any** Redis or Database (PostgreSQL, MySQL, SQL Server) instance, whether it's running in Docker, on the host, or a managed cloud service.

### Custom UI & Localization

The static files (HTML, CSS, JS) and i18n properties can be fully customized. You can mount a volume with your own templates.

*   **Guide**: [Custom UI Guide](https://github.com/KuneiFormApp/wedge-custom-login)

### API Key Configuration

WedgeAuth supports API key validation for secure communication with the User Provider API. When enabled, all requests to the User Provider endpoint will include the configured API key in the specified header.

| Variable | Description | Default | Example |
|----------|-------------|---------|---------|
| `API_KEY_MUST_BE_VALIDATED` | Enable/disable API key validation | `false` | `true` |
| `CUSTOM_API_KEY_HEADER` | Header name for API key | `X-API-Key` | `Authorization` |
| `CUSTOM_API_KEY` | API key value for authentication | (empty) | `your-secure-api-key` |

**Example Configuration:**
```yaml
environment:
  API_KEY_MUST_BE_VALIDATED: true
  CUSTOM_API_KEY_HEADER: X-API-Key
  CUSTOM_API_KEY: prod-api-key-2024
```

When enabled, WedgeAuth will add the following header to User Provider API requests:
```
X-API-Key: your-secure-api-key
```

### Circuit Breaker Configuration

WedgeAuth uses Resilience4j Circuit Breaker to protect against cascading failures when calling external services like the User Provider API. The circuit breaker automatically opens when failure rates exceed thresholds.

| Variable | Description | Default | Example |
|----------|-------------|---------|---------|
| `CIRCUIT_BREAKER_ENABLED` | Enable/disable circuit breaker | `true` | `true` |
| `CIRCUIT_BREAKER_FAILURE_RATE_THRESHOLD` | Failure rate percentage to open circuit (0-100) | `50` | `60` |
| `CIRCUIT_BREAKER_MIN_CALLS` | Minimum calls before calculating failure rate | `10` | `20` |
| `CIRCUIT_BREAKER_HALF_OPEN_CALLS` | Permitted calls in half-open state | `5` | `10` |
| `CIRCUIT_BREAKER_WAIT_DURATION_MS` | Wait time in open state (ms) before half-open | `60000` | `30000` |
| `CIRCUIT_BREAKER_SLIDING_WINDOW_MS` | Sliding window duration (ms) | `10000` | `60000` |
| `CIRCUIT_BREAKER_WINDOW_TYPE` | Window type: `count_based` or `time_based` | `count_based` | `time_based` |
| `CIRCUIT_BREAKER_AUTO_TRANSITION` | Auto transition from open to half-open | `true` | `false` |

**Circuit Breaker States:**

1. **CLOSED**: Normal operation, calls pass through
2. **OPEN**: All calls fail immediately (circuit is open)
3. **HALF_OPEN**: Limited calls allowed to test recovery

**Example Configuration for Aggressive Fallback:**
```yaml
environment:
  CIRCUIT_BREAKER_ENABLED: true
  CIRCUIT_BREAKER_FAILURE_RATE_THRESHOLD: 30    # Open at 30% failures
  CIRCUIT_BREAKER_MIN_CALLS: 5                  # Evaluate after 5 calls
  CIRCUIT_BREAKER_HALF_OPEN_CALLS: 3             # Test with 3 calls
  CIRCUIT_BREAKER_WAIT_DURATION_MS: 15000        # Wait 15 seconds
  CIRCUIT_BREAKER_SLIDING_WINDOW_MS: 5000        # 5-second window
  CIRCUIT_BREAKER_WINDOW_TYPE: time_based        # Time-based sliding window
```

---

## Alternative Configurations

### YAML-Only Configuration

You can run WedgeAuth without a database using just the YAML configuration (Environment variables). Clients are defined in-memory.

```yaml
services:
  wedge-auth:
    image: ariandel007/wedge-authorization-server:0.0.1
    environment:
      CLIENT_STORAGE_TYPE: "yaml"
      
      # Define Clients via Env Vars
      WEDGE_CLIENTS_0_ID: "public-spa"
      WEDGE_CLIENTS_0_SECRET: "none"
      WEDGE_CLIENTS_0_REDIRECT_URIS: "http://localhost:3000/callback"
      WEDGE_CLIENTS_0_SCOPES: "openid,profile"
      
      WEDGE_CLIENTS_1_ID: "backend-service"
      WEDGE_CLIENTS_1_SECRET: "s3cret"
      # ...
```

### Multi-Tenant Configuration (Experimental)

> [!WARNING]
> Multi-tenant configuration via YAML is currently **experimental**. Use Database-backed storage for stable multi-tenant deployments.

```yaml
    environment:
      # Enable Multi-tenancy
      IS_MULTITENANT_ENABLED: true
      
      # Define Tenants
      WEDGE_TENANTS_0_ID: "tenant-a"
      WEDGE_TENANTS_0_NAME: "Tenant A"
      WEDGE_TENANTS_0_ISSUER: "http://auth.example.com/tenant-a"
      
      WEDGE_TENANTS_1_ID: "tenant-b"
      WEDGE_TENANTS_1_NAME: "Tenant B"
```

---

## Database Reference

### Supported Databases

WedgeAuth supports standard JDBC databases. Configure `CLIENT_DB_DRIVER` accordingly:

*   **PostgreSQL**: `org.postgresql.Driver`
*   **MySQL**: `com.mysql.cj.jdbc.Driver`
*   **SQL Server**: `com.microsoft.sqlserver.jdbc.SQLServerDriver`

### SQL Scripts

Use these SQL examples to populate your database (PostgreSQL syntax shown, adjust for others). Note that `created_at` and `updated_at` columns are managed automatically if your schema supports it, or you can omit them for defaults.

#### Tenants

**Table Purpose**: The `tenants` table isolates data and configurations for different organizations or logical groups. Even in a single-tenant setup, a 'default' tenant is required to hold the configuration for the User Provider.

```sql
INSERT INTO tenants
(id, "name", user_provider_endpoint, user_provider_timeout, mfa_registration_endpoint)
VALUES
('default', 'Default Local Tenant', 'http://[IP_OR_DNS]:8081/api/users/validate', 5000, 'http://[IP_OR_DNS]:8081/api/users/{userId}/mfa');
```

#### OAuth Clients

**Table Purpose**: The `oauth_clients` table registers the applications (web, mobile, backend services) that are allowed to request tokens from WedgeAuth. It defines their credentials, allowed scopes, and redirect URIs.

```sql
-- Public Client
INSERT INTO oauth_clients
(id, client_id, client_secret, client_name, client_authentication_methods, authorization_grant_types, redirect_uris, post_logout_redirect_uris, scopes, require_authorization_consent, require_pkce, tenant_id, image_url, access_url)
VALUES
(1, 'public-local-spa-client', ' ', 'Public Local SPA Client', 'none', 'authorization_code,refresh_token', 'http://localhost:3000/callback,http://localhost:3000/silent-renew', 'http://localhost:3000/', 'openid,profile,email,read,write,offline_access', true, true, 'default', NULL, 'http://localhost:3000/');

-- Confidential Client
INSERT INTO oauth_clients
(id, client_id, client_secret, client_name, client_authentication_methods, authorization_grant_types, redirect_uris, post_logout_redirect_uris, scopes, require_authorization_consent, require_pkce, tenant_id, image_url, access_url)
VALUES
(2, 'confidential-local-spa-client', '$2a$12$WH/kWfXCCnApG0rA21bYfuoFauMqG/eErNAM0xrhBpAFtvayed.Q.', 'Confidential Local Client', 'client_secret_basic,client_secret_post', 'authorization_code,refresh_token', 'http://localhost:3000/callback,http://localhost:3000/silent-renew', 'http://localhost:3000/', 'openid,profile,email,read,write,offline_access', false, true, 'default', NULL, NULL);
```

### Table Descriptions

| Table | Field | Description |
|-------|-------|-------------|
| **tenants** | `id` | Unique identifier for the tenant (e.g., 'default'). |
| | `name` | Human-readable name of the tenant. |
| | `user_provider_endpoint` | URL where WedgeAuth sends user validation requests. |
| | `mfa_registration_endpoint` | URL for registering MFA secrets. |
| **oauth_clients** | `client_id` | Unique Public Identifier for the client application. |
| | `client_secret` | Hashed secret (BCrypt) for confidential clients. Empty for public. |
| | `redirect_uris` | Comma-separated list of allowed callback URLs. |
| | `scopes` | Allowed OAuth2 scopes (e.g., `openid`, `profile`). |
| | `require_pkce` | Whether PKCE is enforced (Recommended: `true`). |
| | `tenant_id` | Foreign key linking the client to a specific tenant. |

---

## API Contracts

WedgeAuth relies on your external **User Provider API** for user validation, MFA, and scopes validation.

### User Provider API

**Request (Validation):**
`POST {USER_PROVIDER_ENDPOINT}`
```json
{
  "username": "user@example.com",
  "password": "plain-text-password",
  "scopes": ["openid", "profile"]
}
```

**Response (Contract):**

```json
{
  "userId": "unique-user-id",
  "username": "user@example.com",
  "email": "user@example.com",
  "metadata": {                       // Custom Claims Map
    "role": "admin",                  // Any key-value pairs here will be...
    "department": "IT",               // ...added to the Access Token payload.
    "subscription": "premium"
  },
  "mfaEnabled": true,                 // Master switch for MFA
  "mfaData": {
    "twoFaRegistered": true,          // true = User has setup MFA; false = Needs setup
    "mfaSecret": "BASE32SECRET",      // REQUIRED if twoFaRegistered=true it is not finded the MFA set up will trigger and the auth server will create one
    "mfaKeyId": "WedgeAuth:user"      // Label for Authenticator App
  }
}
```
The mfaSecret should be encrypted when is stored and only decrypted when is in transit

### Scopes Validation API

WedgeAuth can call an external endpoint to validate if a user is authorized for a given set of scopes. This is used during the OAuth2 authorization flow.

**Endpoint:** `POST {USER_PROVIDER_SCOPES_VALIDATION_ENDPOINT}`
- Template: `http://localhost:8081/api/users/{userId}/scopes`
- `{userId}` is replaced with the actual user ID.

**Request:**
```json
{
  "scopes": ["openid", "admin"]
}
```

**Response (Success):**
HTTP 200 OK (empty body) if scopes are valid.

**Response (Failure):**
HTTP 403 Forbidden (empty body) if scopes are not valid.

### MFA Registration API

When a user completes MFA setup, WedgeAuth sends the generated secret to your MFA registration endpoint.

**Request (MFA Registration):**
`PATCH {MFA_REGISTRATION_ENDPOINT}`

The endpoint URL is templated with `{userId}`, for example:
- Template: `http://localhost:8081/api/users/{userId}/mfa`
- Actual call: `http://localhost:8081/api/users/unique-user-id/mfa`

```json
{
  "mfaSecret": "BASE32ENCODEDSECRET",
  "mfaKeyId": "WedgeAuth:user@example.com"
}
```

**Response (Success):**
Empty 204

> [!NOTE]
> The `{userId}` placeholder in the `mfa_registration_endpoint` URL is automatically replaced with the actual user ID from the validation response.

### MFA Logic

The behavior of Multi-Factor Authentication depends on the response fields:

1.  **MFA Disabled**: 
    If `"mfaEnabled": false`, MFA is completely skipped for this user.
    
2.  **MFA Setup Required**:
    If `"mfaEnabled": true` AND `"twoFaRegistered": false`.
    *   WedgeAuth will generate a secret and redirect the user to the MFA Setup page.
    *   After setup, WedgeAuth calls your `mfa_registration_endpoint` (PATCH) to save the secret.

3.  **MFA Verification**:
    If `"mfaEnabled": true` AND `"twoFaRegistered": true`.
    *   WedgeAuth expects the `mfaSecret` to be present in the response.
    *   The user is redirected to the MFA verification page to enter their TOTP code.

### Error Response Format

When the User Provider API encounters errors, it should respond with the following standardized error format:

**Response (Error):**
HTTP Status: 4xx/5xx

```json
{
  "errorCodes": [
    "INVALID_CREDENTIALS" // this value should be mapped in the i18n files
  ],
  "messages": [
    "Human-readable error message"
  ],
  "errorDate": "2026-01-22T17:58:14Z"
}
```

#### Error Codes Reference

| Error Code | Description | Example Message |
|------------|-------------|----------------|
| `INVALID_CREDENTIALS` | Username or password is incorrect | "Invalid username or password" |
| `USER_NOT_FOUND` | User does not exist in the system | "User not found" |
| `USER_DISABLED` | User account is disabled | "User account is disabled" |
| `USER_LOCKED` | User account is locked | "User account is locked" |
| `USER_EXPIRED` | User account has expired | "User account has expired" |
| `INVALID_REQUEST` | Request format is invalid | "Invalid request format" |
| `MISSING_USERNAME` | Username field is missing | "Username is required" |
| `MISSING_PASSWORD` | Password field is missing | "Password is required" |
| `INVALID_USERNAME_FORMAT` | Username format is invalid | "Invalid username format" |
| `INVALID_PASSWORD_FORMAT` | Password format is invalid | "Invalid password format" |
| `MFA_REGISTRATION_FAILED` | MFA registration failed | "MFA registration failed" |
| `MFA_INVALID_SECRET` | MFA secret is invalid | "Invalid MFA secret" |
| `MFA_INVALID_KEY_ID` | MFA key ID is invalid | "Invalid MFA key ID" |
| `MFA_ALREADY_REGISTERED` | MFA is already registered | "MFA is already registered" |
| `MFA_NOT_SUPPORTED` | MFA is not supported | "MFA is not supported" |
| `SERVICE_UNAVAILABLE` | Service is temporarily unavailable | "User provider service is unavailable" |
| `TIMEOUT_ERROR` | Request timed out | "Request timeout" |
| `NETWORK_ERROR` | Network error occurred | "Network error occurred" |
| `INTERNAL_SERVER_ERROR` | Internal server error | "Internal server error" |
| `DATABASE_ERROR` | Database error occurred | "Database error occurred" |
| `UNAUTHORIZED_CLIENT` | Client is not authorized | "Unauthorized client" |
| `FORBIDDEN_OPERATION` | Operation is forbidden | "Operation is forbidden" |
| `RATE_LIMIT_EXCEEDED` | Rate limit was exceeded | "Rate limit exceeded" |
| `INVALID_ENDPOINT` | User provider endpoint is invalid | "Invalid user provider endpoint" |
| `MISSING_CONFIGURATION` | Configuration is missing | "Missing user provider configuration" |

#### Internationalization

Error messages should support internationalization. The `messages` array can contain localized messages based on the client's `Accept-Language` header or a specified locale parameter.

**Example with multiple errors:**
```json
{
  "errorCodes": [
    "INVALID_REQUEST",
    "MISSING_USERNAME"
  ],
  "messages": [
    "Invalid request format",
    "Username is required"
  ],
  "errorDate": "2026-01-22T17:58:14Z"
}
```

> [!IMPORTANT]
> All error responses from the User Provider API must follow this exact format for WedgeAuth to properly parse and handle errors. The `errorDate` should be an ISO 8601 timestamp in UTC.
