-- MySQL Migration Script: Create Tenants and OAuth Clients Tables
-- This script creates both the tenants table and oauth_clients table

-- Create tenants table
CREATE TABLE IF NOT EXISTS tenants (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    user_provider_endpoint VARCHAR(500) NOT NULL,
    user_provider_timeout INT NOT NULL DEFAULT 5000,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create oauth_clients table
CREATE TABLE IF NOT EXISTS oauth_clients (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_id VARCHAR(255) NOT NULL UNIQUE,
    client_secret VARCHAR(500), -- Null for public clients
    client_name VARCHAR(255) NOT NULL,
    
    -- Store collections as comma-separated strings
    client_authentication_methods TEXT NOT NULL,
    authorization_grant_types TEXT NOT NULL,
    redirect_uris TEXT,
    post_logout_redirect_uris TEXT,
    scopes TEXT,
    
    -- Client settings
    require_authorization_consent BOOLEAN NOT NULL DEFAULT false,
    require_pkce BOOLEAN NOT NULL DEFAULT false,
    
    -- Tenant reference for user provider
    tenant_id VARCHAR(255),
    
    -- Audit fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (tenant_id) REFERENCES tenants(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create indexes
CREATE INDEX idx_oauth_clients_client_id ON oauth_clients(client_id);
CREATE INDEX idx_oauth_clients_tenant_id ON oauth_clients(tenant_id);
