package com.coderace.dto;

import java.util.List;

/**
 * Filter criteria for generating problems
 */
public record ProblemFilter(
        String description // Free-form description of the desired problem (e.g., "a tree problem with
                           // dynamic programming")
) {
    // Default constructor for no filtering
    public static ProblemFilter noFilter() {
        return new ProblemFilter("");
    }

    // Check if description is provided
    public boolean hasDescription() {
        return description != null && !description.trim().isEmpty();
    }
}
