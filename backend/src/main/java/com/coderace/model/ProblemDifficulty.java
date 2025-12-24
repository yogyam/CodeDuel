package com.coderace.model;

/**
 * Enum representing problem difficulty levels
 * Rating ranges are used internally for AI guidance, not shown to users
 */
public enum ProblemDifficulty {
    EASY("Easy", 800, 1200),
    MEDIUM("Medium", 1300, 1600),
    HARD("Hard", 1700, 2000),
    EXPERT("Expert", 2100, 2500);

    private final String displayName;
    private final int minRating;
    private final int maxRating;

    ProblemDifficulty(String displayName, int minRating, int maxRating) {
        this.displayName = displayName;
        this.minRating = minRating;
        this.maxRating = maxRating;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMinRating() {
        return minRating;
    }

    public int getMaxRating() {
        return maxRating;
    }

    /**
     * Get average rating for this difficulty level
     * Used to guide AI on appropriate problem difficulty
     */
    public int getAverageRating() {
        return (minRating + maxRating) / 2;
    }
}
