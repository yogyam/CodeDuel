package com.coderace.model;

/**
 * Enum representing the validation status of a generated problem
 */
public enum ValidationStatus {
    /**
     * Problem passed all validation checks
     */
    PASSED,

    /**
     * Problem failed validation checks
     */
    FAILED,

    /**
     * Validation was skipped (e.g., validation disabled in config)
     */
    SKIPPED,

    /**
     * Validation is currently in progress
     */
    IN_PROGRESS
}
