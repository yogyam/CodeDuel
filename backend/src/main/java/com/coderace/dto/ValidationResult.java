package com.coderace.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO representing the result of problem validation
 * Used to determine if a generated problem is high-quality and solvable
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResult {

    /**
     * Whether the problem passed all validation checks
     */
    private boolean isValid;

    /**
     * List of validation errors (e.g., "Test case 3 failed", "No solution found")
     */
    private List<String> errors = new ArrayList<>();

    /**
     * List of warnings (non-blocking issues)
     */
    private List<String> warnings = new ArrayList<>();

    /**
     * Result of solution verification (if solution was generated and tested)
     */
    private SolutionVerificationResult solutionResult;

    /**
     * Quality score from 0-100 based on multiple factors:
     * - Test coverage
     * - Solution correctness
     * - Description clarity
     * - Difficulty alignment
     */
    private double qualityScore;

    /**
     * Add an error to the validation result
     */
    public void addError(String error) {
        this.errors.add(error);
        this.isValid = false;
    }

    /**
     * Add a warning to the validation result
     */
    public void addWarning(String warning) {
        this.warnings.add(warning);
    }

    /**
     * Check if validation has any errors
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * Check if validation has any warnings
     */
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }
}
