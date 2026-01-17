## MFA Flow

### 1. First-Time MFA Setup

```mermaid
sequenceDiagram
    participant U as User
    participant A as Auth Server
    participant P as User Provider API
    participant App as Authenticator App

    U->>A: Login (username/password)
    A->>P: POST /auth/validate
    P-->>A: {mfaEnabled: true, twoFaRegistered: false, mfaKeyId: "..."}
    A-->>U: Redirect to /mfa/setup
    U->>A: GET /mfa/setup
    A->>A: Generate TOTP secret
    A-->>U: Show QR code + manual entry
    U->>App: Scan QR code
    App-->>U: Display 6-digit code
    U->>A: POST /mfa/setup/verify (code)
    A->>A: Validate TOTP code
    A->>P: PUT /users/{userId}/mfa (secret, twoFaRegistered: true)
    P-->>A: Success
    A-->>U: Redirect to /login?mfa_registered
```

### 2. Subsequent Logins with MFA

```mermaid
sequenceDiagram
    participant U as User
    participant A as Auth Server
    participant P as User Provider API
    participant App as Authenticator App

    U->>A: Login (username/password)
    A->>P: POST /auth/validate
    P-->>A: {mfaEnabled: true, twoFaRegistered: true, mfaSecret: "SECRET"}
    A-->>U: Redirect to /mfa/verify
    U->>App: Check authenticator app
    App-->>U: Display current 6-digit code
    U->>A: POST /mfa/verify (code)
    A->>A: Validate TOTP code against stored secret
    A-->>U: Complete authentication & redirect to OAuth flow
```

---

## Configuration Requirements

### Tenant Configuration (YAML)

Add MFA registration endpoint to tenant configuration:

```yaml
wedge:
  tenants:
    - id: "default-tenant"
      name: "Default Tenant"
      userProviderPort:
        endpoint: "http://localhost:8080/api/v1/auth/validate"
        timeout: 5000
        mfaRegistrationEndpoint: "http://localhost:8080/api/v1/users/{userId}/mfa"
```

### Tenant Configuration (Database)

```sql
UPDATE tenants 
SET mfa_registration_endpoint = 'http://localhost:8080/api/v1/users/{userId}/mfa'
WHERE id = 'your-tenant-id';
```

### User Provider API Requirements

Your user provider API must:

1. **Return MFA fields in authentication responses**:
   ```json
   {
     "userId": "user123",
     "username": "user@example.com",
     "email": "user@example.com",
     "metadata": {},
     "mfaEnabled": true,
     "mfaData": {
       "twoFaRegistered": false,
       "mfaKeyId": "WedgeAuth:user@example.com",
       "mfaSecret": null
     }
   }
   ```

2. **Implement MFA registration endpoint**:
   ```
   PUT/PATCH /api/v1/users/{userId}/mfa
   ```
   
   Request body:
   ```json
   {
     "mfaSecret": "BASE32ENCODEDSECRET",
     "twoFaRegistered": true,
     "mfaKeyId": "WedgeAuth:user@example.com"
   }
   ```

3. **Security requirements**:
   - Store `mfaSecret` encrypted at rest
   - Only return secret when `twoFaRegistered=false` (during setup)
   - Never return secret in subsequent logins when `twoFaRegistered=true`

---
