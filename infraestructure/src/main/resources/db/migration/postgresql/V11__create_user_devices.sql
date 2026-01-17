-- Migration: Create user_devices table for device tracking
-- Description: Tracks user devices/browsers with active refresh tokens

CREATE TABLE user_devices (
    device_id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    device_name VARCHAR(255),
    user_agent TEXT,
    ip_address VARCHAR(45),
    first_seen TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    authorization_id VARCHAR(255)
);

-- Index for efficient user device lookups
CREATE INDEX idx_user_devices_user_id ON user_devices(user_id);
CREATE INDEX idx_user_devices_authorization_id ON user_devices(authorization_id);
