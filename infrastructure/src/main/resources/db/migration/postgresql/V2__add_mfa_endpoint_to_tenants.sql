-- PostgreSQL Migration: Add MFA registration endpoint to tenants table
-- This column stores the URL template for the MFA registration endpoint
-- which the authorization server will call after successful MFA verification

ALTER TABLE tenants 
ADD COLUMN mfa_registration_endpoint VARCHAR(500);

COMMENT ON COLUMN tenants.mfa_registration_endpoint IS 
'URL template for MFA registration endpoint. Placeholder {userId} will be replaced with actual user ID. Example: http://localhost:8080/api/v1/users/{userId}/mfa';
