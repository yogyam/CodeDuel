package com.coderace.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO for submission verification verdict
 * Contains results of testing code against all test cases
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionVerdict {

    private int testsPassed;
    private int totalTests;
    private String verdict; // ACCEPTED, WRONG_ANSWER, RUNTIME_ERROR, etc.
    private List<String> errors = new ArrayList<>();
    private String firstFailedTest; // Input that caused first failure

    public SubmissionVerdict(int testsPassed, int totalTests, List<String> errors) {
        this.testsPassed = testsPassed;
        this.totalTests = totalTests;
        this.errors = errors != null ? errors : new ArrayList<>();
        this.verdict = determineVerdict();
    }

    public boolean isAccepted() {
        return testsPassed == totalTests && totalTests > 0;
    }

    private String determineVerdict() {
        if (testsPassed == totalTests && totalTests > 0) {
            return "ACCEPTED";
        }
        if (!errors.isEmpty()) {
            String firstError = errors.get(0).toLowerCase();
            if (firstError.contains("runtime") || firstError.contains("error")) {
                return "RUNTIME_ERROR";
            }
            if (firstError.contains("timeout") || firstError.contains("time limit")) {
                return "TIME_LIMIT_EXCEEDED";
            }
        }
        return "WRONG_ANSWER";
    }

    public double getAccuracy() {
        return totalTests > 0 ? (double) testsPassed / totalTests * 100 : 0.0;
    }
}
