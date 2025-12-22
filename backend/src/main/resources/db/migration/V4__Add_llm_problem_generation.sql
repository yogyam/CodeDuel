-- Migration V4: Add LLM Problem Generation Support
-- Adds fields for LLM-generated problems and modernizes schema

-- Add new columns for LLM-generated problems
ALTER TABLE problems ADD COLUMN IF NOT EXISTS source VARCHAR(20) DEFAULT 'CODEFORCES';
ALTER TABLE problems ADD COLUMN IF NOT EXISTS llm_model VARCHAR(50);
ALTER TABLE problems ADD COLUMN IF NOT EXISTS generated_at TIMESTAMP;
ALTER TABLE problems ADD COLUMN IF NOT EXISTS is_verified BOOLEAN DEFAULT false;
ALTER TABLE problems ADD COLUMN IF NOT EXISTS sample_input TEXT;
ALTER TABLE problems ADD COLUMN IF NOT EXISTS sample_output TEXT;

-- Mark existing problems as legacy Codeforces problems
UPDATE problems SET source = 'CODEFORCES' WHERE source IS NULL;

-- Create index for faster lookups of generated problems
CREATE INDEX IF NOT EXISTS idx_problems_source ON problems(source);
CREATE INDEX IF NOT EXISTS idx_problems_rating_tags ON problems(rating, tags);
