package com.coderace.constants;

/**
 * Constants for game-related messages and status updates
 * Centralizes all user-facing strings to avoid magic strings throughout the
 * codebase
 */
public class GameConstants {

    // Game state messages
    public static final String MSG_GAME_STARTED = "Game started! Solve the problem.";
    public static final String MSG_CODE_ACCEPTED = "Code accepted! You won!";
    public static final String MSG_ERROR_PROBLEM_NOT_FOUND = "Error: Problem not found. Please try generating again.";

    // Generation status messages
    public static final String STATUS_GENERATING_OPTIONS = "Generating problem options...";
    public static final String STATUS_OPTIONS_READY = "Problem options ready!";
    public static final String STATUS_GENERATING_PROBLEM = "Generating full problem...";
    public static final String STATUS_GENERATING_AI = "Generating problem with AI (this may take 10-30s)...";
    public static final String STATUS_BUILDING_PROMPT = "Building problem prompt...";
    public static final String STATUS_PROCESSING_DETAILS = "Processing problem details...";
    public static final String STATUS_CREATING_TESTS = "Creating test cases...";
    public static final String STATUS_PROBLEM_READY = "Problem ready!";
    public static final String STATUS_FAILED_GENERATION = "Failed to generate problem";
    public static final String STATUS_FAILED_TITLES = "Failed to generate title options";

    // Room identifiers for non-WebSocket operations
    public static final String ROOM_ID_REST_API = "REST_API";

    private GameConstants() {
        // Private constructor to prevent instantiation
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
