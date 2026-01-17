-- Create OAuth2 Authorization Consent table for tracking user consent
-- This table stores which clients a user has authorized and what scopes they granted
CREATE TABLE oauth2_authorization_consent (
    registered_client_id VARCHAR(100) NOT NULL,
    principal_name VARCHAR(200) NOT NULL,
    authorities VARCHAR(1000) NOT NULL,
    PRIMARY KEY (registered_client_id, principal_name)
);

CREATE INDEX idx_consent_principal ON oauth2_authorization_consent(principal_name);
