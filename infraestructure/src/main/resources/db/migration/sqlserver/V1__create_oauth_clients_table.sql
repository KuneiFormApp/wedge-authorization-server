-- SQL Server Migration Script: Create OAuth Clients Table
-- This script creates the oauth_clients table for storing OAuth 2.1 client configurations

IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[oauth_clients]') AND type in (N'U'))
BEGIN
    CREATE TABLE oauth_clients (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        client_id NVARCHAR(255) NOT NULL UNIQUE,
        client_secret NVARCHAR(500), -- Null for public clients
        client_name NVARCHAR(255) NOT NULL,
        
        -- Store collections as comma-separated strings
        client_authentication_methods NVARCHAR(MAX) NOT NULL, -- e.g., "none" or "client_secret_basic,client_secret_post"
        authorization_grant_types NVARCHAR(MAX) NOT NULL, -- e.g., "authorization_code,refresh_token"
        redirect_uris NVARCHAR(MAX), -- Comma-separated URIs
        post_logout_redirect_uris NVARCHAR(MAX), -- Comma-separated URIs
        scopes NVARCHAR(MAX), -- Comma-separated scopes
        
        -- Client settings
        require_authorization_consent BIT NOT NULL DEFAULT 0,
        require_pkce BIT NOT NULL DEFAULT 0,
        
        -- User provider configuration
        user_provider_enabled BIT NOT NULL DEFAULT 1,
        user_provider_endpoint NVARCHAR(500),
        user_provider_timeout INT DEFAULT 5000,
        
        -- Audit fields
        created_at DATETIME2 DEFAULT GETDATE(),
        updated_at DATETIME2 DEFAULT GETDATE()
    );
END;
GO

-- Create index on client_id for fast lookups
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_oauth_clients_client_id' AND object_id = OBJECT_ID('oauth_clients'))
BEGIN
    CREATE INDEX idx_oauth_clients_client_id ON oauth_clients(client_id);
END;
GO

-- Create trigger to update updated_at timestamp
IF EXISTS (SELECT * FROM sys.triggers WHERE name = 'trg_oauth_clients_updated_at')
BEGIN
    DROP TRIGGER trg_oauth_clients_updated_at;
END;
GO

CREATE TRIGGER trg_oauth_clients_updated_at
ON oauth_clients
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    UPDATE oauth_clients
    SET updated_at = GETDATE()
    FROM oauth_clients oc
    INNER JOIN inserted i ON oc.id = i.id;
END;
GO
