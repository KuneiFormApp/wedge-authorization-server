-- PostgreSQL Migration Script: Create Tenants and OAuth Clients Tables
-- This script creates both the tenants table and oauth_clients table

-- Create tenants table
CREATE TABLE IF NOT EXISTS tenants (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    user_provider_endpoint VARCHAR(500) NOT NULL,
    user_provider_timeout INTEGER NOT NULL DEFAULT 5000,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create oauth_clients table
CREATE TABLE IF NOT EXISTS oauth_clients (
    id BIGSERIAL PRIMARY KEY,
    client_id VARCHAR(255) NOT NULL UNIQUE,
    client_secret VARCHAR(500), -- Null for public clients
    client_name VARCHAR(255) NOT NULL,
    
    -- Store collections as comma-separated strings
    client_authentication_methods TEXT NOT NULL, -- e.g., "none" or "client_secret_basic,client_secret_post"
    authorization_grant_types TEXT NOT NULL, -- e.g., "authorization_code,refresh_token"
    redirect_uris TEXT, -- Comma-separated URIs
    post_logout_redirect_uris TEXT, -- Comma-separated URIs
    scopes TEXT, -- Comma-separated scopes
    
    -- Client settings
    require_authorization_consent BOOLEAN NOT NULL DEFAULT false,
    require_pkce BOOLEAN NOT NULL DEFAULT false,
    
    -- Tenant reference for user provider
    tenant_id VARCHAR(255) REFERENCES tenants(id),
    
    -- Audit fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_oauth_clients_client_id ON oauth_clients(client_id);
CREATE INDEX IF NOT EXISTS idx_oauth_clients_tenant_id ON oauth_clients(tenant_id);

-- Create trigger to update updated_at timestamp for tenants
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_tenants_updated_at BEFORE UPDATE ON tenants
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_oauth_clients_updated_at BEFORE UPDATE ON oauth_clients
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
