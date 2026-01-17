# Docker Compose Examples

Ready-to-use Docker Compose configurations for different deployment scenarios.

## Table of Contents

1. [Standalone Setup](#standalone-setup)
2. [Customizable Setup](#customizable-setup)
3. [Distributed Sessions](#distributed-sessions)
4. [Database-Backed](#database-backed)
5. [Full Stack](#full-stack)

> **💡 Important:** These Docker Compose files are provided as **examples to show how environment variables should be configured**. You don't need to run PostgreSQL or Redis in Docker - simply use `docker run` with environment variables pointing to your existing infrastructure. See the [Deployment Guide](./deployment-guide.md) for `docker run` examples.

---

## Standalone Setup

**File:** `docker-compose.standalone.yml`

Perfect for quick testing, POC, and local development.

```yaml
version: '3.8'

services:
  wedge-auth:
    image: wedge-auth:jvm
    container_name: wedge-auth-standalone
    ports:
      - "9001:9001"
    environment:
      WEDGE_AUTH_SERVER_PORT: 9001
      LOGGING_LEVEL_ROOT: INFO
      LOGGING_LEVEL_APP: DEBUG
    restart: unless-stopped
```

**Usage:**
```bash
docker-compose -f docker-compose.standalone.yml up
```

---

## Customizable Setup

**File:** `docker-compose.customizable.yml`

Includes environment-specific client configurations and optional user service.

```yaml
version: '3.8'

services:
  wedge-auth:
    image: wedge-auth:jvm
    container_name: wedge-auth-customizable
    ports:
      - "9001:9001"
    depends_on:
      - user-service
    environment:
      # Server
      WEDGE_AUTH_SERVER_PORT: 9001
      JWT_ISSUER: http://localhost:9001
      
      # Custom Clients
      PUBLIC_CLIENT_ID: dev-spa-app
      PUBLIC_CLIENT_REDIRECT_URIS: http://localhost:3000/callback,http://localhost:3000/silent-renew
      PUBLIC_CLIENT_POST_LOGOUT_URIS: http://localhost:3000/
      
      CONFIDENTIAL_CLIENT_ID: dev-backend
      CONFIDENTIAL_CLIENT_SECRET: dev-secret-123
      CONFIDENTIAL_CLIENT_REDIRECT_URIS: http://localhost:8082/callback
      
      # User Provider
      USER_PROVIDER_ENDPOINT: http://user-service:8081/api/users/validate
      USER_PROVIDER_TIMEOUT: 5000
      
      # Logging
      LOGGING_LEVEL_ROOT: INFO
      LOGGING_LEVEL_APP: DEBUG
      LOGGING_LEVEL_SECURITY: DEBUG
    restart: unless-stopped

  user-service:
    # Replace with your actual user service image
    image: your-user-service:latest
    container_name: user-service-customizable
    ports:
      - "8081:8081"
    environment:
      SERVER_PORT: 8081
    restart: unless-stopped
```

**Usage:**
```bash
docker-compose -f docker-compose.customizable.yml up
```

---

## Distributed Sessions

**File:** `docker-compose.distributed.yml`

Redis-backed session storage for multi-instance deployments and horizontal scaling.

```yaml
version: '3.8'

services:
  wedge-auth:
    image: wedge-auth:jvm
    container_name: wedge-auth-distributed
    ports:
      - "9001:9001"
    depends_on:
      - redis
    environment:
      # Server
      WEDGE_AUTH_SERVER_PORT: 9001
      JWT_ISSUER: https://auth.example.com
      
      # Session Storage - Redis
      SESSION_STORAGE_TYPE: redis
      REDIS_HOST: redis
      REDIS_PORT: 6379
      REDIS_PASSWORD: redis-password
      REDIS_USERNAME: default
      REDIS_DATABASE: 0
      
      # Session Idle Timeouts (reset on each request)
      AUTH_SESSION_TTL: 600
      HTTP_SESSION_TIMEOUT: 1800
      
      # Redis Namespaces
      AUTH_SESSION_NAMESPACE: wedge:auth:session
      HTTP_SESSION_NAMESPACE: wedge:http:session
      REDIS_TOKEN_NAMESPACE: wedge:oauth2:auth
      
      # Logging
      LOGGING_LEVEL_ROOT: INFO
      LOGGING_LEVEL_APP: DEBUG
    restart: unless-stopped

  redis:
    image: redis:7-alpine
    container_name: redis-distributed
    command: redis-server --requirepass redis-password
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data
    restart: unless-stopped

volumes:
  redis-data:
    driver: local
```

**Usage:**
```bash
# Start services
docker-compose -f docker-compose.distributed.yml up -d

# Scale authorization server
docker-compose -f docker-compose.distributed.yml up -d --scale wedge-auth=3

# View logs
docker-compose -f docker-compose.distributed.yml logs -f wedge-auth
```

---

## Database-Backed

**File:** `docker-compose.database.yml`

Production-ready setup with PostgreSQL for client storage and persistent JWT keys.

```yaml
version: '3.8'

services:
  wedge-auth:
    image: wedge-auth:jvm
    container_name: wedge-auth-database
    ports:
      - "9001:9001"
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_started
    volumes:
      # Mount JWT keys (create these first!)
      - ./keys/private-key.pem:/app/keys/private-key.pem:ro
      - ./keys/public-key.pem:/app/keys/public-key.pem:ro
    environment:
      # Server
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
      CLIENT_DB_POOL_SIZE: 10
      CLIENT_DB_MIN_IDLE: 5
      
      # Session Storage - Redis
      SESSION_STORAGE_TYPE: redis
      REDIS_HOST: redis
      REDIS_PORT: 6379
      REDIS_PASSWORD: ${REDIS_PASSWORD}
      REDIS_USERNAME: default
      REDIS_DATABASE: 0
      
      # Session Idle Timeouts (reset on each request)
      AUTH_SESSION_TTL: 600
      HTTP_SESSION_TIMEOUT: 3600
      
      # JWT - File-based keys
      JWT_KEY_TYPE: file
      JWT_KEY_ID: production-key-2024
      JWT_PRIVATE_KEY_PATH: /app/keys/private-key.pem
      JWT_PUBLIC_KEY_PATH: /app/keys/public-key.pem
      
      # Token Configuration
      IS_REFRESH_TOKEN_ENABLED: true
      ACCESS_TOKEN_TTL: 1800
      REFRESH_TOKEN_TTL: 2592000
      
      # Logging
      LOGGING_LEVEL_ROOT: WARN
      LOGGING_LEVEL_APP: INFO
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:9001/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s

  postgres:
    image: postgres:16-alpine
    container_name: postgres-database
    environment:
      POSTGRES_DB: oauth2_clients
      POSTGRES_USER: wedge_user
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres-data:/var/lib/postgresql/data
    ports:
      - "5432:5432"
    restart: unless-stopped
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U wedge_user -d oauth2_clients"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    container_name: redis-database
    command: redis-server --requirepass ${REDIS_PASSWORD}
    volumes:
      - redis-data:/data
    ports:
      - "6379:6379"
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "redis-cli", "--raw", "incr", "ping"]
      interval: 10s
      timeout: 3s
      retries: 5

volumes:
  postgres-data:
    driver: local
  redis-data:
    driver: local
```

**Environment File:** `.env`

```bash
# Database credentials
DB_PASSWORD=your-secure-database-password

# Redis credentials
REDIS_PASSWORD=your-secure-redis-password
```

**Setup:**

```bash
# 1. Generate RSA keys
mkdir -p keys
openssl genrsa -out keys/private-key.pem 2048
openssl rsa -in keys/private-key.pem -pubout -out keys/public-key.pem

# 2. Create .env file with secrets
cat > .env << EOF
DB_PASSWORD=$(openssl rand -base64 32)
REDIS_PASSWORD=$(openssl rand -base64 32)
EOF

# 3. Start services
docker-compose -f docker-compose.database.yml up -d

# 4. Check health
docker-compose -f docker-compose.database.yml ps
```

---

## Full Stack

**File:** `docker-compose.fullstack.yml`

Complete setup with database, Redis, custom UI, and optional admin tools.

```yaml
version: '3.8'

services:
  wedge-auth:
    image: wedge-auth:jvm
    container_name: wedge-auth-fullstack
    ports:
      - "9001:9001"
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_started
    volumes:
      # JWT Keys
      - ./keys/private-key.pem:/app/keys/private-key.pem:ro
      - ./keys/public-key.pem:/app/keys/public-key.pem:ro
      
      # Custom UI
      - ./custom-ui/templates:/app/custom-ui/templates:ro
      - ./custom-ui/static:/app/custom-ui/static:ro
      - ./custom-ui/i18n:/app/custom-ui/i18n:ro
      
    environment:
      # Server
      WEDGE_AUTH_SERVER_PORT: 9001
      JWT_ISSUER: https://auth.example.com
      
      # Client Storage
      CLIENT_STORAGE_TYPE: postgresql
      CLIENT_DB_URL: jdbc:postgresql://postgres:5432/oauth2_clients
      CLIENT_DB_USERNAME: wedge_user
      CLIENT_DB_PASSWORD: ${DB_PASSWORD}
      CLIENT_DB_DRIVER: org.postgresql.Driver
      CLIENT_DB_SCHEMA: public
      CLIENT_DB_AUTO_CREATE_SCHEMA: true
      CLIENT_DB_POOL_SIZE: 20
      CLIENT_DB_MIN_IDLE: 10
      
      # Session Storage
      SESSION_STORAGE_TYPE: redis
      REDIS_HOST: redis
      REDIS_PORT: 6379
      REDIS_PASSWORD: ${REDIS_PASSWORD}
      REDIS_USERNAME: default
      REDIS_DATABASE: 0
      
      # JWT
      JWT_KEY_TYPE: file
      JWT_KEY_ID: production-key-2024
      JWT_PRIVATE_KEY_PATH: /app/keys/private-key.pem
      JWT_PUBLIC_KEY_PATH: /app/keys/public-key.pem
      
      # Tokens
      IS_REFRESH_TOKEN_ENABLED: true
      ACCESS_TOKEN_TTL: 1800
      REFRESH_TOKEN_TTL: 2592000
      
      # Custom Frontend
      FRONTEND_TEMPLATES_PATH: file:///app/custom-ui/templates
      FRONTEND_STATIC_PATH: file:///app/custom-ui/static
      FRONTEND_I18N_BASENAME: file:///app/custom-ui/i18n/messages
      FRONTEND_DEFAULT_LOCALE: en
      FRONTEND_SUPPORTED_LOCALES: en,es,fr
      
      # Virtual Threads
      SPRING_THREADS_VIRTUAL_ENABLED: true
      
      # Logging
      LOGGING_LEVEL_ROOT: WARN
      LOGGING_LEVEL_APP: INFO
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:9001/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s

  postgres:
    image: postgres:16-alpine
    container_name: postgres-fullstack
    environment:
      POSTGRES_DB: oauth2_clients
      POSTGRES_USER: wedge_user
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres-data:/var/lib/postgresql/data
    restart: unless-stopped
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U wedge_user -d oauth2_clients"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    container_name: redis-fullstack
    command: redis-server --requirepass ${REDIS_PASSWORD}
    volumes:
      - redis-data:/data
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "redis-cli", "--raw", "incr", "ping"]
      interval: 10s
      timeout: 3s
      retries: 5

  # Optional: PostgreSQL Admin UI
  pgadmin:
    image: dpage/pgadmin4:latest
    container_name: pgadmin-fullstack
    environment:
      PGADMIN_DEFAULT_EMAIL: admin@example.com
      PGADMIN_DEFAULT_PASSWORD: ${PGADMIN_PASSWORD}
    ports:
      - "5050:80"
    volumes:
      - pgadmin-data:/var/lib/pgadmin
    restart: unless-stopped

  # Optional: Redis Commander
  redis-commander:
    image: rediscommander/redis-commander:latest
    container_name: redis-commander-fullstack
    environment:
      REDIS_HOSTS: local:redis:6379:0:${REDIS_PASSWORD}
    ports:
      - "8081:8081"
    restart: unless-stopped

volumes:
  postgres-data:
  redis-data:
  pgadmin-data:
```

**Environment File:** `.env`

```bash
# Database
DB_PASSWORD=your-secure-database-password

# Redis
REDIS_PASSWORD=your-secure-redis-password

# PgAdmin (optional)
PGADMIN_PASSWORD=admin-password
```

**Directory Structure:**

```
deployment/
├── docker-compose.full.yml
├── .env
├── keys/
│   ├── private-key.pem
│   └── public-key.pem
└── custom-ui/
    ├── templates/
    │   ├── login.html
    │   └── consent.html
    ├── static/
    │   ├── css/
    │   │   └── custom.css
    │   ├── js/
    │   │   └── custom.js
    │   └── images/
    │       └── logo.png
    └── i18n/
        ├── messages_en.properties
        └── messages_es.properties
```

**Setup:**

```bash
# 1. Create directory structure
mkdir -p deployment/{keys,custom-ui/{templates,static/{css,js,images},i18n}}
cd deployment

# 2. Generate keys
openssl genrsa -out keys/private-key.pem 2048
openssl rsa -in keys/private-key.pem -pubout -out keys/public-key.pem

# 3. Create custom UI files (see CUSTOM_LOGIN_TUTORIAL.md)

# 4. Create .env file
cat > .env << EOF
DB_PASSWORD=$(openssl rand -base64 32)
REDIS_PASSWORD=$(openssl rand -base64 32)
PGADMIN_PASSWORD=$(openssl rand -base64 16)
EOF

# 5. Start all services
docker-compose -f docker-compose.fullstack.yml up -d

# 6. Access services
# - WedgeAuth: http://localhost:9001
# - PgAdmin: http://localhost:5050
# - Redis Commander: http://localhost:8081
```

---

## Useful Commands

### View Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f wedge-auth

# Last 100 lines
docker-compose logs --tail=100 wedge-auth
```

### Database Management

```bash
# Connect to PostgreSQL
docker-compose exec postgres psql -U wedge_user -d oauth2_clients

# Backup database
docker-compose exec postgres pg_dump -U wedge_user oauth2_clients > backup.sql

# Restore database
docker-compose exec -T postgres psql -U wedge_user -d oauth2_clients < backup.sql
```

### Redis Management

```bash
# Connect to Redis CLI
docker-compose exec redis redis-cli -a your-password

# List all keys
docker-compose exec redis redis-cli -a your-password KEYS '*'

# Monitor commands
docker-compose exec redis redis-cli -a your-password MONITOR
```

### Health Checks

```bash
# Check all services
docker-compose ps

# Check WedgeAuth health
curl http://localhost:9001/actuator/health

# Check PostgreSQL
docker-compose exec postgres pg_isready -U wedge_user

# Check Redis
docker-compose exec redis redis-cli -a your-password ping
```

### Cleanup

```bash
# Stop services
docker-compose down

# Stop and remove volumes (WARNING: deletes data)
docker-compose down -v

# Remove everything including images
docker-compose down -v --rmi all
```

---

## Next Steps

- [Deployment Guide](./deployment-guide.md) - Detailed scenario explanations
- [Quick Start](./quick-start.md) - Get started in 5 minutes
- [Custom Login Tutorial](./CUSTOM_LOGIN_TUTORIAL.md) - Customize the UI
