package com.coderace.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO representing the result of verifying a generated solution against test
 * cases
 * Used during problem validation to ensure the problem is solvable
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SolutionVerificationResult {

    /**
     * The LLM-generated solution code that was tested
     */
    private String generatedSolution;

    /**
     * Programming language of the solution
     */
    private String language;

    /**
     * Number of test cases that passed
     */
    private int testsPassed;

    /**
     * Total number of test cases
     */
    private int totalTests;

    /**
     * Total execution time in milliseconds
     */
    private long executionTimeMs;

    /**
     * Whether all test cases passed
     */
    private boolean allPassed;

    /**
     * List of failure reasons if any tests failed
     */
    private List<String> failureReasons = new ArrayList<>();

    /**
     * Add a failure reason
     */
    public void addFailureReason(String reason) {
        this.failureReasons.add(reason);
    }

    /**
     * Get pass percentage
     */
    public double getPassPercentage() {
        if (totalTests == 0)
            return 0.0;
        return (testsPassed * 100.0) / totalTests;
    }

    /**
     * Check if solution is valid (all tests passed)
     */
    public boolean isValid() {
        return allPassed && testsPassed == totalTests;
    }
}
