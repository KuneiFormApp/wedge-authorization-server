# WedgeAuth Quick Start Guide

Get WedgeAuth running in under 5 minutes with zero configuration!

## Prerequisites

- Docker installed
- Port 9001 available

## Step 1: Build the Image

```bash
# Navigate to project directory
cd wedge-authorization-server

# Build the Docker image
docker build -t wedge-auth:jvm .
```

## Step 2: Run the Container

```bash
docker run -p 9001:9001 wedge-auth:jvm
```

That's it! 🎉

## What You Get

Your authorization server is now running at `http://localhost:9001` with:

### Pre-configured OAuth Clients

#### 1. Public SPA Client
- **Client ID:** `public-spa-client`
- **Client Secret:** None (public client)
- **Grant Types:** `authorization_code`, `refresh_token`
- **Redirect URIs:** 
  - `http://localhost:3000/callback`
  - `http://localhost:3000/silent-renew`
- **Scopes:** `openid`, `profile`, `email`, `read`, `write`, `offline_access`
- **PKCE:** Required ✅

#### 2. Confidential Backend Client
- **Client ID:** `confidential-backend-client`
- **Client Secret:** `secret`
- **Grant Types:** `client_credentials`, `authorization_code`, `refresh_token`
- **Redirect URIs:** `http://localhost:8082/callback`
- **Scopes:** `openid`, `profile`, `email`, `admin`, `offline_access`
- **PKCE:** Required ✅

### Default Configuration

- ✅ In-memory session storage
- ✅ In-memory token storage
- ✅ Auto-generated RSA keys for JWT signing
- ✅ Default login UI (Thymeleaf)
- ✅ Virtual threads enabled (Java 21+)

## Testing the Authorization Flow

### 1. Authorization Code Flow (for SPAs)

Open your browser and navigate to:

```
http://localhost:9001/oauth2/authorize?
  response_type=code&
  client_id=public-spa-client&
  redirect_uri=http://localhost:3000/callback&
  scope=openid%20profile%20email&
  code_challenge=YOUR_CODE_CHALLENGE&
  code_challenge_method=S256
```

> **Note:** For PKCE testing, see [oauth2-pkce-testing-guide.md](../oauth2-pkce-testing-guide.md)

### 2. Client Credentials Flow (for backend services)

```bash
curl -X POST http://localhost:9001/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -u "confidential-backend-client:secret" \
  -d "grant_type=client_credentials&scope=admin"
```

## Customizing the Setup

### Change the Port

```bash
docker run -p 8080:8080 \
  -e WEDGE_AUTH_SERVER_PORT=8080 \
  wedge-auth:jvm
```

### Use Custom Client Credentials

```bash
docker run -p 9001:9001 \
  -e PUBLIC_CLIENT_ID=my-app \
  -e PUBLIC_CLIENT_REDIRECT_URIS=http://localhost:4200/callback \
  -e CONFIDENTIAL_CLIENT_SECRET=my-super-secret \
  wedge-auth:jvm
```

### Enable Debug Logging

```bash
docker run -p 9001:9001 \
  -e LOGGING_LEVEL_ROOT=DEBUG \
  -e LOGGING_LEVEL_APP=TRACE \
  wedge-auth:jvm
```

## Important Notes

### ⚠️ Development Only

This minimal setup is perfect for:
- Local development
- Testing OAuth flows
- Proof of concept
- Learning OAuth2/OIDC

### ❌ Not for Production

This setup has limitations:
- Sessions lost on restart
- JWT keys regenerated on restart (invalidates tokens)
- No horizontal scaling
- No persistent client storage

## Next Steps

### For Development Teams

See [Scenario 2: Customizable](./deployment-guide.md#scenario-2-customizable) for:
- Custom client configurations
- External user provider integration
- Enhanced logging

### For Staging/Production

See the full [Deployment Guide](./deployment-guide.md) for:
- Distributed Sessions (Redis session storage)
- Database-Backed (PostgreSQL client storage)
- Full Stack (Custom UI branding)
- High-availability setups

## Troubleshooting

### Container Won't Start

```bash
# Check logs
docker logs <container-id>

# Verify port is available
netstat -an | grep 9001
```

### Can't Access the Server

```bash
# Verify container is running
docker ps

# Check container health
docker inspect <container-id>
```

### Need to Reset Everything

```bash
# Stop and remove container
docker stop <container-id>
docker rm <container-id>

# Start fresh
docker run -p 9001:9001 wedge-auth:jvm
```

## Useful Endpoints

| Endpoint | Description |
|----------|-------------|
| `http://localhost:9001/.well-known/openid-configuration` | OpenID Connect discovery |
| `http://localhost:9001/oauth2/authorize` | Authorization endpoint |
| `http://localhost:9001/oauth2/token` | Token endpoint |
| `http://localhost:9001/oauth2/jwks` | JSON Web Key Set |
| `http://localhost:9001/actuator/health` | Health check |
| `http://localhost:9001/actuator/info` | Application info |

## Learn More

- [Full Deployment Guide](./deployment-guide.md) - All deployment scenarios
- [OAuth2 PKCE Testing Guide](../oauth2-pkce-testing-guide.md) - Test OAuth flows
- [Custom Login Tutorial](./CUSTOM_LOGIN_TUTORIAL.md) - Customize the UI
- [RSA Key Generation](../key_generation_rsa.md) - Generate JWT keys

---

**Happy coding! 🚀**
