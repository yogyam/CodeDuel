-- Migration V3: Add In-Browser IDE Support
-- Adds tables for test cases and code submissions

-- Create test_cases table
CREATE TABLE test_cases (
    id BIGSERIAL PRIMARY KEY,
    problem_id VARCHAR(20) NOT NULL,
    input TEXT NOT NULL,
    expected_output TEXT NOT NULL,
    type VARCHAR(50) NOT NULL,
    is_hidden BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index for efficient problem lookup
CREATE INDEX idx_test_cases_problem ON test_cases(problem_id);

-- Create submissions table
CREATE TABLE submissions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    game_room_id VARCHAR(10) NOT NULL,
    problem_id VARCHAR(20) NOT NULL,
    code TEXT NOT NULL,
    language VARCHAR(50) NOT NULL,
    verdict VARCHAR(50),
    tests_passed INTEGER,
    total_tests INTEGER,
    error_message TEXT,
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for efficient queries
CREATE INDEX idx_submissions_user ON submissions(user_id);
CREATE INDEX idx_submissions_room ON submissions(game_room_id);
CREATE INDEX idx_submissions_problem ON submissions(problem_id);
CREATE INDEX idx_submissions_submitted_at ON submissions(submitted_at DESC);
