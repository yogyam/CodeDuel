-- Remove codeforces_handle column from users table
ALTER TABLE users DROP COLUMN IF EXISTS codeforces_handle;
