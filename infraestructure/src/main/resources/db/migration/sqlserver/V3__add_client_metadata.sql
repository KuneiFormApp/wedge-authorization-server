-- Add image_url and access_url to oauth_clients table
ALTER TABLE oauth_clients
ADD image_url VARCHAR(500),
ADD access_url VARCHAR(500);
