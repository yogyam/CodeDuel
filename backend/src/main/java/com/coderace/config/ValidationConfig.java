package com.coderace.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for problem validation settings
 * Configurable via application.properties with prefix "validation"
 */
@Configuration
@ConfigurationProperties(prefix = "validation")
@Data
public class ValidationConfig {

    /**
     * Whether validation is enabled globally
     * Set to false to disable validation entirely
     */
    private boolean enabled = true;

    /**
     * Maximum number of retries if validation fails
     * After this many failed attempts, return error to user
     */
    private int maxRetries = 2;

    /**
     * Whether to require solution verification
     * If true, generates and tests a reference solution
     * If false, only validates format and structure
     */
    private boolean requireSolutionVerification = true;

    /**
     * Minimum quality score (0-100) to pass validation
     * Problems below this score will be regenerated
     */
    private int minQualityScore = 70;

    /**
     * Timeout in seconds for validation process
     * Prevents validation from hanging indefinitely
     */
    private int timeoutSeconds = 30;

    /**
     * Delay in milliseconds between Piston API calls
     * Required to respect rate limit (1 request per 200ms)
     */
    private int pistonRateLimitMs = 250;
}
