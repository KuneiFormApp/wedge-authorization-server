# Logout Usage Guide

This guide explains how to implement logout functionality in your client applications when using WedgeAuth as your authorization server.

## Overview

WedgeAuth provides two logout mechanisms:

1. **Standard Form Logout** (`/logout`) - For browser-based applications
2. **OIDC RP-Initiated Logout** (`/connect/logout`) - For OpenID Connect clients

Both mechanisms trigger **OAuth2 authorization revocation**, immediately invalidating refresh tokens.

> [!IMPORTANT]
> **Access Token Behavior**
> 
> JWT access tokens remain valid until their natural expiration. For better security, configure short-lived access tokens (3-5 minutes) in production.
> 
> ```bash
> ACCESS_TOKEN_TTL=300  # 5 minutes (recommended)
> ```

---

## Method 1: Standard Form Logout

### For Browser-Based Applications

This is the simplest method for traditional web applications.

**Logout URL:**
```
GET /logout
```

**Behavior:**
1. Clears HTTP session
2. Revokes all OAuth2 authorizations for the user
3. Redirects to `/login?logout=true` with success message

**Example (HTML):**
```html
<a href="/logout">Sign Out</a>
```

**Example (JavaScript):**
```javascript
function logout() {
    window.location.href = '/logout';
}
```

---

## Method 2: OIDC RP-Initiated Logout

### For OpenID Connect Clients

This method follows the [OIDC RP-Initiated Logout specification](https://openid.net/specs/openid-connect-rpinitiated-1_0.html).

**Logout URL:**
```
GET /connect/logout?id_token_hint=<token>&post_logout_redirect_uri=<uri>&state=<state>
```

### Parameters

| Parameter | Required | Description |
|-----------|----------|-------------|
| `id_token_hint` | Recommended | The ID Token previously issued to the client |
| `post_logout_redirect_uri` | Optional | Where to redirect after logout (must be registered) |
| `client_id` | Recommended | Client ID (required if `post_logout_redirect_uri` is provided) |
| `state` | Optional | Opaque value to maintain state between logout request and callback |

### Registering Post-Logout Redirect URIs

**Environment Variable:**
```bash
PUBLIC_CLIENT_POST_LOGOUT_URIS=https://app.example.com/,https://app.example.com/goodbye
CONFIDENTIAL_CLIENT_POST_LOGOUT_URIS=https://backend.example.com/
```

**application.yaml:**
```yaml
wedge:
  clients:
    - client-id: my-spa-client
      post-logout-redirect-uris:
        - https://app.example.com/
        - https://app.example.com/goodbye
```

### Example: JavaScript SPA

```javascript
async function logout() {
    const idToken = localStorage.getItem('id_token');
    const postLogoutRedirectUri = 'https://app.example.com/';
    const state = generateRandomState();
    
    // Store state for validation
    sessionStorage.setItem('logout_state', state);
    
    // Construct logout URL
    const logoutUrl = new URL('https://auth.example.com/connect/logout');
    logoutUrl.searchParams.set('id_token_hint', idToken);
    logoutUrl.searchParams.set('post_logout_redirect_uri', postLogoutRedirectUri);
    logoutUrl.searchParams.set('client_id', 'my-spa-client');
    logoutUrl.searchParams.set('state', state);
    
    // Clear local tokens
    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');
    localStorage.removeItem('id_token');
    
    // Redirect to logout endpoint
    window.location.href = logoutUrl.toString();
}

function generateRandomState() {
    return Math.random().toString(36).substring(2, 15);
}
```

### Example: React Application

```typescript
import { useAuth } from './auth-context';

function LogoutButton() {
    const { idToken, clientId, authServerUrl } = useAuth();
    
    const handleLogout = () => {
        const logoutUrl = new URL(`${authServerUrl}/connect/logout`);
        logoutUrl.searchParams.set('id_token_hint', idToken);
        logoutUrl.searchParams.set('post_logout_redirect_uri', window.location.origin);
        logoutUrl.searchParams.set('client_id', clientId);
        
        // Clear tokens from storage
        localStorage.clear();
        
        // Redirect to logout
        window.location.href = logoutUrl.toString();
    };
    
    return <button onClick={handleLogout}>Sign Out</button>;
}
```

### Example: Angular Application

```typescript
import { Injectable } from '@angular/core';
import { Router } from '@angular/router';

@Injectable({ providedIn: 'root' })
export class AuthService {
    private authServerUrl = 'https://auth.example.com';
    private clientId = 'my-angular-app';
    
    constructor(private router: Router) {}
    
    logout(): void {
        const idToken = localStorage.getItem('id_token');
        const postLogoutRedirectUri = `${window.location.origin}/`;
        
        const logoutUrl = new URL(`${this.authServerUrl}/connect/logout`);
        logoutUrl.searchParams.set('id_token_hint', idToken || '');
        logoutUrl.searchParams.set('post_logout_redirect_uri', postLogoutRedirectUri);
        logoutUrl.searchParams.set('client_id', this.clientId);
        
        // Clear tokens
        localStorage.removeItem('access_token');
        localStorage.removeItem('refresh_token');
        localStorage.removeItem('id_token');
        
        // Redirect
        window.location.href = logoutUrl.toString();
    }
}
```

---

## Security Considerations

### 1. Short-Lived Access Tokens

Since JWT access tokens cannot be revoked before expiration, use short TTLs:

**Recommended for Production:**
```bash
ACCESS_TOKEN_TTL=300        # 5 minutes
REFRESH_TOKEN_TTL=86400     # 1 day
```

### 2. Validate Post-Logout Redirect URIs

Only registered URIs are allowed. Attempting to redirect to an unregistered URI will result in an error.

**Example Error:**
```
Invalid post_logout_redirect_uri: https://malicious.com
```

### 3. Clear Client-Side Tokens

Always clear tokens from client storage (localStorage, sessionStorage, cookies) before redirecting to the logout endpoint.

```javascript
// Clear all tokens
localStorage.removeItem('access_token');
localStorage.removeItem('refresh_token');
localStorage.removeItem('id_token');
sessionStorage.clear();
```

### 4. Use HTTPS in Production

Always use HTTPS for logout endpoints in production:

```bash
JWT_ISSUER=https://auth.example.com
```

---

## Testing Logout

### Manual Testing

1. **Obtain tokens via OAuth2 flow**
2. **Verify tokens work** (make API calls with access token)
3. **Log out** (via `/logout` or `/connect/logout`)
4. **Verify refresh token is revoked**:
   ```bash
   curl -X POST https://auth.example.com/oauth2/token \
     -d "grant_type=refresh_token" \
     -d "refresh_token=<old_refresh_token>" \
     -d "client_id=my-client"
   
   # Expected: {"error": "invalid_grant"}
   ```
5. **Verify access token still works** (until expiration)

### Automated Testing

```javascript
describe('Logout', () => {
    it('should revoke refresh token on logout', async () => {
        // 1. Login and get tokens
        const { accessToken, refreshToken, idToken } = await login();
        
        // 2. Verify refresh token works
        const newTokens = await refreshAccessToken(refreshToken);
        expect(newTokens.access_token).toBeDefined();
        
        // 3. Logout
        await logout(idToken);
        
        // 4. Verify refresh token is revoked
        await expect(refreshAccessToken(refreshToken))
            .rejects.toThrow('invalid_grant');
    });
});
```

---

## Troubleshooting

### Issue: "Invalid post_logout_redirect_uri"

**Cause:** The redirect URI is not registered for the client.

**Solution:** Add the URI to the client configuration:
```bash
PUBLIC_CLIENT_POST_LOGOUT_URIS=https://app.example.com/,https://app.example.com/goodbye
```

### Issue: Refresh token still works after logout

**Cause:** Logout handler not configured or principal indexing not working.

**Solution:** Verify `OAuth2AuthorizationRevocationLogoutHandler` is wired in `SecurityConfig`:
```java
.logout(logout -> logout
    .addLogoutHandler(logoutHandler)
    .logoutSuccessUrl("/login?logout=true")
    .permitAll())
```

### Issue: Access token still works after logout

**Expected Behavior:** JWT access tokens remain valid until expiration.

**Solution:** Use shorter access token TTLs (3-5 minutes) for production.

---

## Related Documentation

- [Environment Variables](./environment-variables.md) - Configuration reference
- [Deployment Guide](./deployment-guide.md) - Production deployment
- [OIDC RP-Initiated Logout Spec](https://openid.net/specs/openid-connect-rpinitiated-1_0.html) - Official specification
