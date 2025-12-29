-- Fix column name mismatch in refresh_tokens table
-- Production database has 'expiry_date' but code expects 'expires_at'

-- Check if the column 'expiry_date' exists and rename it to 'expires_at'
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_name = 'refresh_tokens' 
        AND column_name = 'expiry_date'
    ) THEN
        ALTER TABLE refresh_tokens RENAME COLUMN expiry_date TO expires_at;
    END IF;
END $$;
