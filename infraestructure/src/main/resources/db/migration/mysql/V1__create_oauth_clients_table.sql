-- MySQL Migration Script: Create OAuth Clients Table
-- This script creates the oauth_clients table for storing OAuth 2.1 client configurations

CREATE TABLE IF NOT EXISTS oauth_clients (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
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
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create index on client_id for fast lookups
CREATE INDEX idx_oauth_clients_client_id ON oauth_clients(client_id);
