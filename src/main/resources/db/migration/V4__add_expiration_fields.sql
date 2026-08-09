-- Add expiration and inactive status
ALTER TABLE urls
ADD COLUMN expires_at TIMESTAMP WITH TIME ZONE,
ADD COLUMN inactive BOOLEAN DEFAULT false;

CREATE INDEX idx_expires_at_inactive ON urls(expires_at, inactive);
