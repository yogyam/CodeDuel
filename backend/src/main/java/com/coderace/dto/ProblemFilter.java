package com.coderace.dto;

import java.util.List;

/**
 * Filter criteria for selecting Codeforces problems
 */
public record ProblemFilter(
        Integer minDifficulty, // Minimum rating (e.g., 800, 1000)
        Integer maxDifficulty, // Maximum rating (e.g., 1500, 2000)
        List<String> tags // Algorithm tags (e.g., ["dp", "greedy"])
) {
    // Default constructor for no filtering
    public static ProblemFilter noFilter() {
        return new ProblemFilter(null, null, List.of());
    }

    // Check if any filters are applied
    public boolean hasFilters() {
        return (minDifficulty != null || maxDifficulty != null ||
                (tags != null && !tags.isEmpty()));
    }

    // Get tags as comma-separated string for API
    public String getTagsString() {
        if (tags == null || tags.isEmpty()) {
            return "";
        }
        return String.join(",", tags);
    }
}
