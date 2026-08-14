CREATE TABLE IF NOT EXISTS domains (
    id BIGSERIAL PRIMARY KEY,
    domain VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    active BOOLEAN DEFAULT true
);

ALTER TABLE urls ADD COLUMN domain_id BIGINT REFERENCES domains(id);
ALTER TABLE urls DROP CONSTRAINT IF EXISTS urls_short_code_key;

CREATE UNIQUE INDEX idx_urls_domain_short_code ON urls(domain_id, short_code) WHERE domain_id IS NOT NULL;
CREATE UNIQUE INDEX idx_urls_short_code_default ON urls(short_code) WHERE domain_id IS NULL;
