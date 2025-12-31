-- PostgreSQL Migration Script: Create OAuth Clients Table
-- This script creates the oauth_clients table for storing OAuth 2.1 client configurations

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
    
    -- User provider configuration
    user_provider_enabled BOOLEAN NOT NULL DEFAULT true,
    user_provider_endpoint VARCHAR(500),
    user_provider_timeout INTEGER DEFAULT 5000,
    
    -- Audit fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create index on client_id for fast lookups
CREATE INDEX IF NOT EXISTS idx_oauth_clients_client_id ON oauth_clients(client_id);

-- Create trigger to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_oauth_clients_updated_at BEFORE UPDATE ON oauth_clients
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
