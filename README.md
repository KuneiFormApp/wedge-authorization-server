<img width="967" height="239" alt="image" src="https://github.com/user-attachments/assets/04b755ec-4c1e-4ccf-af44-3595ccc03e2c" />

# 🔐 WedgeAuth

**WedgeAuth** is a **headless OAuth 2.1 / OpenID Connect authorization server** built on **Spring Boot 4.0.1** and **Spring Authorization Server**.

> **WedgeAuth is an authorization server, not an identity system.**  
> **You own your users. You own your data. No vendor lock-in.**

---

## 🎯 What is WedgeAuth?

WedgeAuth is designed for teams that:
- Already have their own user database
- Want OAuth 2.1 / OIDC without the complexity
- Need a secure, production-ready auth server **without vendor lock-in**
- Prefer configuration over code

**We don't believe in locking you into our ecosystem.** WedgeAuth integrates with *your* existing user system via HTTP, doesn't store your users, and can be replaced at any time. Your auth infrastructure should serve you, not trap you.

---

## 🚀 How It Works

1. **User tries to log in** → WedgeAuth shows a login page (or you bring your own UI)
2. **User submits credentials** → WedgeAuth calls *your* HTTP user provider endpoint to validate
3. **Your system validates** → Returns user data (username, email, roles, etc.)
4. **WedgeAuth issues tokens** → JWT access tokens signed with your RSA keys
5. **Your apps verify tokens** → Using the public JWKS endpoint

**No user data is stored in WedgeAuth.** It's a pure authorization layer that delegates authentication to your existing systems.

---

## ✨ What WedgeAuth Does (Now)

### 🔑 Core OAuth 2.1 / OpenID Connect
- ✅ Authorization Code flow with **PKCE required by default**
- ✅ OpenID Connect support
- ✅ JWT access tokens (asymmetric RSA signing only)
- ✅ JWKS endpoint for token verification
- ✅ Token introspection
- ✅ Token revocation
- ✅ Logout with OIDC RP-Initiated Logout support

### 👤 Headless User Authentication
- ✅ **HTTP-based user providers** — integrate with any user system via REST API
- ✅ No local user database required
- ✅ Custom user model mapping via YAML
- ✅ User metadata mapped into JWT claims
- ✅ Per-client user provider configuration

### 🔐 Multi-Factor Authentication (MFA)
- ✅ TOTP-based MFA (Time-based One-Time Passwords)
- ✅ QR code generation for authenticator apps
- ✅ MFA enrollment flow
- ✅ MFA verification during login
- ✅ Per-user MFA configuration

### 🧠 Session Management
- ✅ Redis-backed sessions (recommended for production)
- ✅ In-memory sessions (default, for development)
- ✅ Configurable session TTL
- ✅ Distributed session support for multi-instance deployments

### 🧩 OAuth Client Management
- ✅ **YAML-based client configuration** (default, no database needed)
- ✅ **Database-backed clients** (PostgreSQL, MySQL 8.0+, SQL Server 2012+)
- ✅ Public and confidential clients
- ✅ PKCE enforcement for public clients
- ✅ Configurable redirect URIs and scopes
- ✅ Client secret bcrypt hashing
- ✅ Multi-tenant support (experimental)

### 🖥️ Customizable Login UI
- ✅ Built-in Thymeleaf templates with i18n support
- ✅ **Fully customizable login UI** — bring your own HTML/CSS/JS
- ✅ Multiple pre-built themes available
- ✅ External static file serving
- ✅ See [`docs/CUSTOM_LOGIN_TUTORIAL.md`](https://github.com/KuneiFormApp/wedge-custom-login/blob/main/README.md) for details

### ⚙️ Configuration-First Design
- ✅ YAML or environment variable configuration
- ✅ No database required by default
- ✅ Configurable JWT key management (runtime or file-based)
- ✅ Extensive environment variable support
- ✅ See [`docs/environment-variables.md`](docs/environment-variables.md) for all options

---

## 🚧 What's In Progress

### 🌐 Social Login
> **Status**: Planned, not yet implemented

When implemented, this will:
- Allow OAuth login via Google, GitHub, Microsoft, etc.
- Be fully configurable via YAML or environment variables
- Map external provider data to your user model
- **WedgeAuth will remain the token issuer** (no vendor lock-in)

### 🛡️ Additional Features (Planned)
- ⏳ Audit logging for compliance
- ⏳ Admin UI for client/user management
- ⏳ Rate limiting and brute-force protection

---

## 🚫 What WedgeAuth Does NOT Do

WedgeAuth is **not** a complete identity platform. It deliberately does not:

- ❌ Store or manage users (you own your users)
- ❌ Store passwords (your user provider does that)
- ❌ Act as a resource server
- ❌ Support legacy flows (Implicit, Password Grant)
- ❌ Support symmetric (HMAC) JWT signing

**This is by design.** WedgeAuth is a focused authorization server that integrates with your existing systems.

---

## 🎯 Design Principles

- **No vendor lock-in** — Own your auth, own your users, own your data
- **Headless by design** — Integrate with any frontend or user system
- **Secure by default** — PKCE required, asymmetric signing, bcrypt hashing
- **Configuration over code** — YAML-driven setup, minimal boilerplate
- **Progressive complexity** — Start simple (no DB), scale when needed (Redis, DB)
- **Hexagonal architecture** — Domain logic independent of frameworks
- **Production-ready** — Built on Spring Authorization Server with enterprise patterns

---

## 📚 Documentation

- **[Deployment Guide](docs/deployment-guide.md)** — Get it running easily with Docker
- **[Docker Compose Examples](docs/docker-compose-examples.md)** — Ready-to-use configurations
- **[Environment Variables](docs/environment-variables.md)** — Complete configuration reference
- **[Custom Login Tutorial](docs/CUSTOM_LOGIN_TUTORIAL.md)** — Customize the login UI
- **[MFA User Guide](docs/mfa-user-guide.md)** — Multi-factor authentication setup
- **[MFA Architecture](docs/mfa-architecture.md)** — Technical MFA implementation details
- **[Logout Usage](docs/logout-usage.md)** — OIDC logout flow documentation

---

## 🚀 Quick Start

> **Run first, customize later.**

### Option 1: Docker (Recommended)

See [`docs/deployment-guide.md`](docs/deployment-guide.md) for complete examples with PostgreSQL, Redis, and various configuration modes.

### Option 2: Build from Source

```bash
cd infraestructure
./gradlew bootJar
java -jar build/libs/wedge-authorization-server-*.jar
```

**Default endpoints:**
- Authorization: `http://localhost:9000/oauth2/authorize`
- Token: `http://localhost:9000/oauth2/token`
- JWKS: `http://localhost:9000/oauth2/jwks`
- Login: `http://localhost:9000/login`

---

## 📦 Technology Stack

- **Java 25** (LTS)
- **Spring Boot 4.0.1**
- **Spring Authorization Server**
- **Redis** (optional, for distributed sessions)
- **PostgreSQL / MySQL / SQL Server** (optional, for client storage)
- **Thymeleaf**

---

## 🧪 Ideal Use Cases

- ✅ SPA authentication (Authorization Code + PKCE)
- ✅ Internal authentication infrastructure
- ✅ Microservice ecosystems
- ✅ Teams that already own user data
- ✅ Headless-first systems
- ✅ Organizations avoiding vendor lock-in

---

## 📄 License

**AGPLv3 License**

This means:
- ✅ Free to use, modify, and distribute
- ✅ Source code must remain open if you modify and distribute
- ✅ Network use triggers copyleft (if you run a modified version as a service, you must share your changes)

**We chose AGPLv3 to prevent vendor lock-in at the license level too.** Your auth should be open and auditable.

---

## 🧠 TL;DR

**WedgeAuth authenticates users and issues tokens.**  
**You own your users. You own your data. No lock-in.**

---

<div align="center">

**Built with ❤️ by the KuneiForm Team**

[GitHub](https://github.com/KuneiFormApp)

</div>
