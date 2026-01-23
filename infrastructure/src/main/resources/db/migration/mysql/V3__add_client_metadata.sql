-- Add image_url and access_url to oauth_clients table
ALTER TABLE oauth_clients
ADD COLUMN image_url VARCHAR(500),
ADD COLUMN access_url VARCHAR(500);
