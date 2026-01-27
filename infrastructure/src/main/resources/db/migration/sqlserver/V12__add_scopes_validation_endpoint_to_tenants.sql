-- SQL Server Migration: Add scopes validation endpoint to tenants table
-- This column stores the URL template for the scopes validation endpoint
-- which the authorization server will call during the OAuth2 authorization flow.

ALTER TABLE tenants 
ADD COLUMN scopes_validation_endpoint VARCHAR(500);