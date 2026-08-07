-- Add index for original_url to speed up duplicate checks
CREATE INDEX idx_original_url ON urls(original_url);
