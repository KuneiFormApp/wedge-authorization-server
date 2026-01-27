-- PostgreSQL Migration: Add scopes validation endpoint to tenants table
-- This column stores the URL template for the scopes validation endpoint
-- which the authorization server will call during the OAuth2 authorization flow.

ALTER TABLE tenants 
ADD COLUMN scopes_validation_endpoint VARCHAR(500);

COMMENT ON COLUMN tenants.scopes_validation_endpoint IS 
'URL template for scopes validation endpoint. Placeholder {userId} will be replaced with actual user ID. Example: http://localhost:8080/api/v1/users/{userId}/scopes';