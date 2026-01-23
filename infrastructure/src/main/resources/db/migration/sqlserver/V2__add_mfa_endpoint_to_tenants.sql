-- SQL Server Migration: Add MFA registration endpoint to tenants table
-- This column stores the URL template for the MFA registration endpoint
-- which the authorization server will call after successful MFA verification

ALTER TABLE tenants 
ADD mfa_registration_endpoint VARCHAR(500);

EXEC sp_addextendedproperty 
    @name = N'MS_Description', 
    @value = N'URL template for MFA registration endpoint. Placeholder {userId} will be replaced with actual user ID. Example: http://localhost:8080/api/v1/users/{userId}/mfa',
    @level0type = N'SCHEMA', @level0name = N'dbo',
    @level1type = N'TABLE',  @level1name = N'tenants',
    @level2type = N'COLUMN', @level2name = N'mfa_registration_endpoint';
