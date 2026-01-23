# OAuth 2.1 PKCE Flow Testing Guide

Complete guide for testing WedgeAuth's OAuth 2.1 Authorization Code + PKCE flow.

---

## ✅ Verified Working Example

These exact URIs have been tested and work correctly:

### Step 1: Authorization Request (Browser)

```
http://localhost:9001/oauth2/authorize?response_type=code&client_id=public-spa-client&redirect_uri=http://localhost:3000/callback&scope=openid profile email&code_challenge=yJKaXJNaZiBd_leJpIYE3WL9zgaWq7PyHPdXpReFPUQ&code_challenge_method=S256
```

**What happens:**
1. Browser redirects to login page
2. Enter credentials (e.g., `alice` / `password123`)
3. After login, redirects to: `http://localhost:3000/callback?code=ABC123...`
4. Copy the authorization code from the URL

### Step 2: Token Exchange (Postman/curl)

**URL:** `http://localhost:9001/oauth2/token`  
**Method:** `POST`  
**Content-Type:** `application/x-www-form-urlencoded`

**Body:**
```
grant_type=authorization_code
client_id=public-spa-client
redirect_uri=http://localhost:3000/callback
code=<PASTE_CODE_FROM_STEP_1>
code_verifier=wQAQ-uoK8DufeIeoLTKjdSxNTbChMN2kMmCRTa7XLOw
```

> ⚠️ **Important:** The `code_verifier` must match the value used to generate the `code_challenge` in Step 1

**Response:**
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "Bearer",
  "expires_in": 3600,
  "scope": "openid profile email",
  "id_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

---

## 🔧 Complete Manual Testing

### Prerequisites

1. **WedgeAuth server running:**
   ```bash
   ./gradlew :infrastructure:bootRun
   ```

2. **User provider configured** with user `alice` (or your test user)

3. **Client configured** in `application.yaml`:
   ```yaml
   wedge:
     clients:
       - client-id: public-spa-client
         redirect-uris:
           - http://localhost:3000/callback
         require-pkce: true
   ```

### Generate PKCE Code Verifier & Challenge

#### PowerShell
```powershell
# Generate code verifier (43 random characters)
$codeVerifier = -join ((65..90) + (97..122) + (48..57) | Get-Random -Count 43 | ForEach-Object {[char]$_})
Write-Host "Code Verifier: $codeVerifier"

# Generate code challenge (SHA256 hash, base64url encoded)
$sha256 = [System.Security.Cryptography.SHA256]::Create()
$hash = $sha256.ComputeHash([System.Text.Encoding]::UTF8.GetBytes($codeVerifier))
$codeChallenge = ([Convert]::ToBase64String($hash)).TrimEnd('=').Replace('+', '-').Replace('/', '_')
Write-Host "Code Challenge: $codeChallenge"
```

#### Bash/Git Bash
```bash
CODE_VERIFIER=$(openssl rand -base64 32 | tr -d "=+/" | cut -c1-43)
echo "Code Verifier: $CODE_VERIFIER"

CODE_CHALLENGE=$(echo -n "$CODE_VERIFIER" | openssl dgst -binary -sha256 | openssl base64 | tr -d "=" | tr "+/" "-_")
echo "Code Challenge: $CODE_CHALLENGE"
```

### Full Flow Testing

#### 1. Start Authorization Flow

Open browser and navigate to:
```
http://localhost:9001/oauth2/authorize?response_type=code&client_id=public-spa-client&redirect_uri=http://localhost:3000/callback&scope=openid%20profile%20email&code_challenge=YOUR_CODE_CHALLENGE&code_challenge_method=S256
```

Replace `YOUR_CODE_CHALLENGE` with the value from the previous step.

#### 2. Login

Enter your credentials on the premium login page.
> [!NOTE]
> During this login phase, the requested `scope` parameter (e.g., `openid profile email`) is now sent to the configured `USER_PROVIDER_ENDPOINT` (`/api/users/validate`) as part of the authentication request body. Your external User Provider API can use this information to perform initial scope validation or return user metadata based on the requested scopes.

#### 3. Get Authorization Code

After successful login, you'll be redirected to:
```
http://localhost:3000/callback?code=AUTHORIZATION_CODE
```

The browser will show an error (because nothing is running on port 3000), but **copy the `code` parameter from the URL**.

#### 4. Exchange Code for Token

Using curl:
```bash
curl -X POST http://localhost:9001/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=authorization_code" \
  -d "client_id=public-spa-client" \
  -d "redirect_uri=http://localhost:3000/callback" \
  -d "code=AUTHORIZATION_CODE" \
  -d "code_verifier=YOUR_CODE_VERIFIER"
```

Using PowerShell:
```powershell
$body = @{
    grant_type = "authorization_code"
    client_id = "public-spa-client"
    redirect_uri = "http://localhost:3000/callback"
    code = "AUTHORIZATION_CODE"
    code_verifier = $codeVerifier
}

Invoke-RestMethod -Uri "http://localhost:9001/oauth2/token" `
    -Method POST `
    -Body $body `
    -ContentType "application/x-www-form-urlencoded" | ConvertTo-Json
```

#### 5. Use Access Token

```bash
# Get user info
curl -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  http://localhost:9001/userinfo

# Introspect token
curl -X POST http://localhost:9001/oauth2/introspect \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "token=YOUR_ACCESS_TOKEN" \
  -d "client_id=public-spa-client"
```

---

## 📝 Parameter Reference

### Authorization Request Parameters

| Parameter | Required | Description |
|-----------|----------|-------------|
| `response_type` | ✅ | Must be `code` |
| `client_id` | ✅ | Your OAuth client ID |
| `redirect_uri` | ✅ | Must match registered redirect URI |
| `scope` | ✅ | Space-separated scopes (e.g., `openid profile email`) |
| `code_challenge` | ✅ | Base64URL-encoded SHA256 hash of verifier |
| `code_challenge_method` | ✅ | Must be `S256` |
| `state` | Recommended | Random value for CSRF protection |

### Token Request Parameters

| Parameter | Required | Description |
|-----------|----------|-------------|
| `grant_type` | ✅ | Must be `authorization_code` |
| `client_id` | ✅ | Your OAuth client ID |
| `redirect_uri` | ✅ | Must exactly match authorization request |
| `code` | ✅ | Authorization code from step 1 |
| `code_verifier` | ✅ | Original random value (43 chars) |

---

## 🐛 Troubleshooting

### "Invalid redirect_uri"
- **Cause:** Redirect URI in token request doesn't match authorization request
- **Solution:** Use exact same `redirect_uri` in both requests

### "Invalid authorization code"
- **Cause:** Code already used, expired, or invalid
- **Solution:** Authorization codes are single-use. Start a new authorization flow

### "Invalid code_verifier"
- **Cause:** Verifier doesn't hash to the challenge used in authorization
- **Solution:** Ensure you're using the same `code_verifier` that generated the `code_challenge`

### "User not found"
- **Cause:** User doesn't exist in user provider
- **Solution:** Check user provider logs, verify user exists

### Login redirects to `/` instead of callback
- **Cause:** Started at `/login` instead of `/oauth2/authorize`
- **Solution:** Always start the flow at the authorization endpoint

### PKCE challenge format error
- **Cause:** Code challenge not properly base64url encoded
- **Solution:** Ensure no `=` padding and `+/` are replaced with `-_`

---

## ✅ Valid Scopes

WedgeAuth supports these standard OpenID Connect scopes:

- `openid` - Required for OpenID Connect
- `profile` - User profile information
- `email` - User email address
- `address` - User postal address
- `phone` - User phone number

---

## 🔐 Security Best Practices

1. **Always use PKCE** for public clients (SPAs, mobile apps)
2. **Use `state` parameter** to prevent CSRF attacks
3. **Store code_verifier securely** on the client
4. **Never log tokens or codes** in production
5. **Validate redirect_uri strictly** - must match exactly
6. **Use HTTPS in production** - OAuth without TLS is insecure
7. **Authorization codes expire quickly** - use within 60 seconds

---

## 📊 Expected Token Response

```json
{
  "access_token": "eyJhbGci...JWT",
  "token_type": "Bearer",
  "expires_in": 3600,
  "refresh_token": "optional",
  "scope": "openid profile email",
  "id_token": "eyJhbGci...JWT"
}
```

### Token Types

- **access_token**: Use for API authorization (`Authorization: Bearer <token>`)
- **id_token**: JWT with user identity claims (decode at jwt.io)
- **refresh_token**: Use to get new access tokens (if configured)

---

## 🧪 Automated Testing

For fully automated testing, consider creating a shell script that handles the PKCE challenge generation, browser interaction, and token exchange.

---

## 📚 References

- [RFC 7636 - PKCE](https://datatracker.ietf.org/doc/html/rfc7636)
- [OAuth 2.1 Draft](https://datatracker.ietf.org/doc/html/draft-ietf-oauth-v2-1)
- [OpenID Connect Core](https://openid.net/specs/openid-connect-core-1_0.html)
