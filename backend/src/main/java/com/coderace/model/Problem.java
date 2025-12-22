package com.coderace.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents a coding problem (from Codeforces or LLM-generated)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Problem {
    // Legacy Codeforces fields (optional for generated problems)
    private String contestId;
    private String index;

    // Common fields
    private String problemId; // Unique ID (contestId+index OR GEN-XXXXX)
    private String name;
    private String type;
    private Integer rating;
    private List<String> tags;
    private String description; // HTML content of problem statement

    // LLM Generation fields
    private String source; // "CODEFORCES" or "GENERATED"
    private String llmModel; // e.g., "gemini-pro"
    private LocalDateTime generatedAt;
    private Boolean isVerified; // Admin verification flag
    private String sampleInput; // Sample test input for display
    private String sampleOutput; // Sample test output for display

    /**
     * Returns the full problem URL (Codeforces only)
     */
    public String getProblemUrl() {
        if ("CODEFORCES".equals(source) && contestId != null && index != null) {
            return "https://codeforces.com/problemset/problem/" + contestId + "/" + index;
        }
        return null;
    }

    /**
     * Returns a unique identifier for this problem
     */
    public String getProblemId() {
        if (problemId != null) {
            return problemId;
        }
        // Legacy: construct from contestId + index
        if (contestId != null && index != null) {
            return contestId + index;
        }
        return null;
    }

    /**
     * Sets the problem ID
     */
    public void setProblemId(String problemId) {
        this.problemId = problemId;
    }
}
