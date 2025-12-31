-- SQL Server Migration Script: Create Tenants and OAuth Clients Tables
-- This script creates both the tenants table and oauth_clients table

-- Create tenants table
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'tenants')
BEGIN
    CREATE TABLE tenants (
        id NVARCHAR(255) PRIMARY KEY,
        name NVARCHAR(255) NOT NULL,
        user_provider_endpoint NVARCHAR(500) NOT NULL,
        user_provider_timeout INT NOT NULL DEFAULT 5000,
        created_at DATETIME2 DEFAULT GETDATE(),
        updated_at DATETIME2 DEFAULT GETDATE()
    );
END
GO

-- Create oauth_clients table
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'oauth_clients')
BEGIN
    CREATE TABLE oauth_clients (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        client_id NVARCHAR(255) NOT NULL UNIQUE,
        client_secret NVARCHAR(500), -- Null for public clients
        client_name NVARCHAR(255) NOT NULL,
        
        -- Store collections as comma-separated strings
        client_authentication_methods NVARCHAR(MAX) NOT NULL,
        authorization_grant_types NVARCHAR(MAX) NOT NULL,
        redirect_uris NVARCHAR(MAX),
        post_logout_redirect_uris NVARCHAR(MAX),
        scopes NVARCHAR(MAX),
        
        -- Client settings
        require_authorization_consent BIT NOT NULL DEFAULT 0,
        require_pkce BIT NOT NULL DEFAULT 0,
        
        -- Tenant reference for user provider
        tenant_id NVARCHAR(255),
        
        -- Audit fields
        created_at DATETIME2 DEFAULT GETDATE(),
        updated_at DATETIME2 DEFAULT GETDATE(),
        
        FOREIGN KEY (tenant_id) REFERENCES tenants(id)
    );
END
GO

-- Create indexes
CREATE INDEX idx_oauth_clients_client_id ON oauth_clients(client_id);
CREATE INDEX idx_oauth_clients_tenant_id ON oauth_clients(tenant_id);
GO

-- Create trigger for tenants updated_at
CREATE TRIGGER trg_tenants_updated_at
ON tenants
AFTER UPDATE
AS
BEGIN
    UPDATE tenants
    SET updated_at = GETDATE()
    WHERE id IN (SELECT DISTINCT id FROM inserted);
END
GO

-- Create trigger for oauth_clients updated_at
CREATE TRIGGER trg_oauth_clients_updated_at
ON oauth_clients
AFTER UPDATE
AS
BEGIN
    UPDATE oauth_clients
    SET updated_at = GETDATE()
    WHERE id IN (SELECT DISTINCT id FROM inserted);
END
GO
