package com.coderace.dto;

import com.coderace.model.ProblemCategory;
import com.coderace.model.ProblemDifficulty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;

/**
 * Filter for problem generation using structured selection
 * Replaces free-text description with concrete category and difficulty
 */
public record ProblemFilter(
        @JsonProperty("category") ProblemCategory category,
        @JsonProperty("difficulty") ProblemDifficulty difficulty,
        @JsonProperty("subtype") @Size(max = 100, message = "Subtype must not exceed 100 characters") String subtype // Optional,
                                                                                                                     // can
                                                                                                                     // be
                                                                                                                     // null
) {
    /**
     * Constructor for Jackson deserialization
     */
    @JsonCreator
    public ProblemFilter {
        // Record compact constructor - Jackson will use this
    }

    /**
     * Create filter from string values (for API compatibility)
     */
    public static ProblemFilter create(String categoryName, String difficultyName, String subtype) {
        ProblemCategory cat = ProblemCategory.valueOf(categoryName);
        ProblemDifficulty diff = ProblemDifficulty.valueOf(difficultyName);
        return new ProblemFilter(cat, diff, subtype);
    }

    /**
     * Check if subtype is specified
     */
    public boolean hasSubtype() {
        return subtype != null && !subtype.trim().isEmpty();
    }

    /**
     * Create a default filter (Medium Dynamic Programming)
     */
    public static ProblemFilter defaultFilter() {
        return new ProblemFilter(
                ProblemCategory.DYNAMIC_PROGRAMMING,
                ProblemDifficulty.MEDIUM,
                null);
    }
}
