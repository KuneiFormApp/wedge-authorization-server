# 🔐 WedgeAuth

**WedgeAuth** is a **headless OAuth 2.1 / OpenID Connect authorization server**
built on **Spring Boot 4.0.0** and **Spring Authorization Server**, designed to be:

- secure by default
- configuration-driven
- database-optional
- framework-agnostic at the domain level

> **WedgeAuth is an authorization server, not an identity system.**

---

## ✨ Features

### 🔑 OAuth 2.1 / OpenID Connect
- Authorization Code flow (**PKCE required by default**)
- OpenID Connect support
- JWT access tokens
- **Asymmetric key signing only (RSA / EC)**
- JWKS endpoint
- Token introspection
- Token revocation

---

### 👤 User Authentication (Headless)
- Users are resolved via **HTTP-based user providers**
- No local user database
- Fully decoupled from user persistence
- Custom user model mapping via YAML
- User metadata can be mapped into JWT claims

---

### 🧠 Sessions & State
- Authorization sessions stored in:
    - Redis (recommended)
    - In-memory cache (default)
- Stateless access tokens

---

### 🧩 OAuth Clients
- **Static client configuration (default)**
- One public client out-of-the-box
- PKCE enforced for public clients
- Configurable redirect URIs
- Configurable scopes

Optional (via database):
- Multiple OAuth clients
- Multiple redirect URIs per client
- Confidential clients
- Client revocation

---

### 🖥 Login UI
- Built-in login page (Thymeleaf)
- Customizable templates
- External login UI support
- Headless-first design

---

### 🌐 Social Login (Optional)
- OAuth login via external identity providers:
    - Google
    - Facebook
    - Others (extensible)
- Fully configurable via `application.yml`
- After successful social login:
    - User is resolved
    - Metadata is mapped
    - **WedgeAuth issues its own tokens**

> External providers authenticate the user.  
> **WedgeAuth remains the token issuer.**

---

### ⚙️ Configuration-First
- All features enabled/disabled via YAML
- No database required by default
- Minimal setup for a working OAuth flow
- Progressive complexity model

---

## 🧱 What WedgeAuth **Can Do (Optional Features)**

These features are **disabled by default** and can be enabled by configuration.

- JDBC-based persistence (PostgreSQL)
- Persistent OAuth clients
- Refresh tokens & rotation
- Token revocation persistence
- Multiple login providers
- External login UI
- Multi-tenant support (planned)
- Consent screen (planned)
- Audit logging (planned)

---

## 🚫 What WedgeAuth **Does NOT Do**

- ❌ User management
- ❌ Password storage
- ❌ Identity provider replacement
- ❌ Resource server functionality
- ❌ Implicit flow
- ❌ Symmetric (HMAC) JWT signing
- ❌ Admin UI (for now)
- ❌ Dynamic Client Registration (RFC)

---

## 🎯 Design Principles

- Headless by design
- Secure by default
- Configuration over convention
- Progressive complexity
- Framework as an infrastructure detail
- Hexagonal architecture

---

## 🚀 Quick Start Philosophy

> **Run first, customize later.**

WedgeAuth is designed to:
- start without a database
- expose a secure OAuth flow immediately
- scale only when needed

---
## 📦 Technology Stack

- Java 25
- Spring Boot 4.0.0
- Spring Authorization Server
- Redis (optional)
- PostgreSQL (optional)
- Thymeleaf (optional UI)

---

## 🧪 Ideal Use Cases

- SPA authentication (Authorization Code + PKCE)
- Internal authentication infrastructure
- Microservice ecosystems
- Teams that already own user data
- Headless-first systems

---

## 📄 License

Apache License 2.0

---

## 🧠 TL;DR

**WedgeAuth authenticates users and issues tokens.  
You own your users.**

---

<div align="center">

**Built with ❤️ by the KuneiForm Team**

[GitHub](https://github.com/KuneiFormApp)

</div>
