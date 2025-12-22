-- Migration V4: Add LLM Problem Generation Support  
-- Creates problems table for both Codeforces and LLM-generated problems

CREATE TABLE IF NOT EXISTS problems (
    id BIGSERIAL PRIMARY KEY,
    problem_id VARCHAR(100) UNIQUE NOT NULL,
    
    -- Common fields
    name VARCHAR(500) NOT NULL,
    description TEXT,
    rating INTEGER,
    tags TEXT[],
    
    -- Legacy Codeforces fields (nullable for generated problems)
    contest_id VARCHAR(50),
    index VARCHAR(10),
    type VARCHAR(50),
    
    -- LLM generation fields
    source VARCHAR(20) DEFAULT 'CODEFORCES',
    llm_model VARCHAR(50),
    generated_at TIMESTAMP,
    is_verified BOOLEAN DEFAULT false,
    sample_input TEXT,
    sample_output TEXT,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_problems_problem_id ON problems(problem_id);
CREATE INDEX IF NOT EXISTS idx_problems_source ON problems(source);
CREATE INDEX IF NOT EXISTS idx_problems_rating ON problems(rating);

