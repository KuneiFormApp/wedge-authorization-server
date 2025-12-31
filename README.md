# 🔐 WedgeAuth

**WedgeAuth** is a **headless OAuth 2.1 / OpenID Connect authorization server**
built on **Spring Boot 4.0.1** and **Spring Authorization Server**, designed to be:

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
- **Asymmetric key signing only (RSA)**
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
- Customizable templates located for external login UI support (bring your own UI)
- Headless-first design

---

### 🌐 Social Login *(Coming Soon)*
> **Status**: Planned feature, not yet implemented


When implemented, this will:
- Be fully configurable via `application.yml` or environment variables
- Allow external providers to authenticate users
- Resolve and map user metadata
- **WedgeAuth will remain the token issuer**

---

### ⚙️ Configuration-First
- All features enabled/disabled via YAML or ENV variables
- No database required by default
- Minimal setup for a working OAuth flow
- Progressive complexity model

---

## 🧱 What WedgeAuth **Can Do (Optional Features)**

These features are **disabled by default** and can be enabled by configuration.

- OAuth clients in BD (PostgreSQL, MySQL 8.0+, SQL Server 2012+)
- External login UI
- Distributed sessions (Redis)
- Token revokation in distributed servers (Redis)
- Social login (planned)
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

---

## 🎯 Design Principles

- **Headless by design** — No vendor lock-in
- **Secure by default** — Best practices baked in
- **Configuration over convention** — YAML-driven setup
- **Progressive complexity** — Start simple, scale when needed
- **Framework as an infrastructure detail** — Domain logic stays pure
- **Hexagonal architecture** — Ports and adapters pattern
- **Avoid vendor lock-in** — Own your auth, own your users, own your data

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

AGPLv3 License

---

## 🧠 TL;DR

**WedgeAuth authenticates users and issues tokens.  
You own your users.**

---

<div align="center">

**Built with ❤️ by the KuneiForm Team**

[GitHub](https://github.com/KuneiFormApp)

</div>
