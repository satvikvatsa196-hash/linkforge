-- Drop redundant index that is automatically created by the UNIQUE constraint
DROP INDEX IF EXISTS idx_short_code;
