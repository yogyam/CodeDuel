-- Add validation metadata fields to problems table
ALTER TABLE problems 
ADD COLUMN is_validated BOOLEAN DEFAULT FALSE,
ADD COLUMN validation_status VARCHAR(20),
ADD COLUMN validation_errors TEXT,
ADD COLUMN validated_at TIMESTAMP,
ADD COLUMN quality_score INTEGER;

-- Create index for faster queries on validation status
CREATE INDEX idx_problems_validation_status ON problems(validation_status);

-- Add comments for documentation
COMMENT ON COLUMN problems.is_validated IS 'Whether this problem has been validated';
COMMENT ON COLUMN problems.validation_status IS 'Status: PASSED, FAILED, SKIPPED, or IN_PROGRESS';
COMMENT ON COLUMN problems.validation_errors IS 'JSON array of validation error messages';
COMMENT ON COLUMN problems.validated_at IS 'Timestamp when validation completed';
COMMENT ON COLUMN problems.quality_score IS 'Quality score from 0-100 based on validation results';
