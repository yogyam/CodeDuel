package com.coderace.service;

import com.coderace.config.ValidationConfig;
import com.coderace.dto.GeneratedProblemResponse;
import com.coderace.dto.ProblemFilter;
import com.coderace.dto.SolutionVerificationResult;
import com.coderace.dto.ValidationResult;
import com.coderace.model.TestCase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Service for validating generated programming problems
 * Ensures problems are solvable, high-quality, and appropriate for target
 * difficulty
 */
@Service
@Slf4j
public class ProblemValidationService {

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.url}")
    private String apiUrl;

    @Value("${openai.model}")
    private String model;

    private final ValidationConfig validationConfig;
    private final CodeExecutionService codeExecutionService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public ProblemValidationService(
            ValidationConfig validationConfig,
            CodeExecutionService codeExecutionService,
            RestTemplate restTemplate,
            ObjectMapper objectMapper) {
        this.validationConfig = validationConfig;
        this.codeExecutionService = codeExecutionService;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Main validation orchestrator
     * Validates a generated problem for quality and solvability
     * 
     * @param problem Generated problem to validate
     * @param filter  Original filter used to generate the problem
     * @return ValidationResult with pass/fail status and details
     */
    public ValidationResult validateProblem(GeneratedProblemResponse problem, ProblemFilter filter) {
        if (!validationConfig.isEnabled()) {
            log.info("Validation is disabled, skipping validation");
            return createSkippedResult();
        }

        log.info("Starting validation for problem: {}", problem.getTitle());
        ValidationResult result = new ValidationResult();
        result.setValid(true);

        try {
            // Step 1: Validate structure and format
            validateStructure(problem, result);

            // Step 2: Validate test cases format
            validateTestCases(problem, result);

            // Step 3: Validate skeleton code (basic syntax check)
            validateSkeletonCode(problem, result);

            // Step 4: Generate and verify reference solution (if enabled)
            if (validationConfig.isRequireSolutionVerification() && result.isValid()) {
                SolutionVerificationResult solutionResult = generateAndVerifySolution(problem);
                result.setSolutionResult(solutionResult);

                if (!solutionResult.isValid()) {
                    result.addError("Reference solution failed to pass all test cases");
                }
            }

            // Step 5: Calculate quality score
            double qualityScore = calculateQualityScore(result);
            result.setQualityScore(qualityScore);

            if (qualityScore < validationConfig.getMinQualityScore()) {
                result.addError(String.format("Quality score (%.1f) below minimum threshold (%d)",
                        qualityScore, validationConfig.getMinQualityScore()));
            }

            log.info("Validation complete: valid={}, score={}, errors={}, warnings={}",
                    result.isValid(), result.getQualityScore(),
                    result.getErrors().size(), result.getWarnings().size());

        } catch (Exception e) {
            log.error("Error during validation: {}", e.getMessage(), e);
            result.addError("Validation process failed: " + e.getMessage());
        }

        return result;
    }

    /**
     * Validate problem structure and required fields
     */
    private void validateStructure(GeneratedProblemResponse problem, ValidationResult result) {
        if (problem.getTitle() == null || problem.getTitle().trim().isEmpty()) {
            result.addError("Problem title is missing");
        }

        if (problem.getDescription() == null || problem.getDescription().trim().isEmpty()) {
            result.addError("Problem description is missing");
        }

        if (problem.getInputFormat() == null || problem.getInputFormat().trim().isEmpty()) {
            result.addWarning("Input format is missing or empty");
        }

        if (problem.getOutputFormat() == null || problem.getOutputFormat().trim().isEmpty()) {
            result.addWarning("Output format is missing or empty");
        }

        if (problem.getConstraints() == null || problem.getConstraints().isEmpty()) {
            result.addWarning("No constraints specified");
        }

        if (problem.getTags() == null || problem.getTags().isEmpty()) {
            result.addWarning("No tags specified");
        }
    }

    /**
     * Validate test cases format and structure
     */
    private void validateTestCases(GeneratedProblemResponse problem, ValidationResult result) {
        // Check sample tests
        if (problem.getSampleTests() == null || problem.getSampleTests().isEmpty()) {
            result.addError("No sample test cases provided");
        } else {
            for (int i = 0; i < problem.getSampleTests().size(); i++) {
                GeneratedProblemResponse.SampleTest test = problem.getSampleTests().get(i);
                if (test.getInput() == null || test.getInput().trim().isEmpty()) {
                    result.addError("Sample test " + (i + 1) + " has empty input");
                }
                if (test.getOutput() == null || test.getOutput().trim().isEmpty()) {
                    result.addError("Sample test " + (i + 1) + " has empty output");
                }
            }
        }

        // Check hidden tests
        if (problem.getHiddenTests() == null || problem.getHiddenTests().isEmpty()) {
            result.addError("No hidden test cases provided");
        } else {
            for (int i = 0; i < problem.getHiddenTests().size(); i++) {
                GeneratedProblemResponse.HiddenTest test = problem.getHiddenTests().get(i);
                if (test.getInput() == null || test.getInput().trim().isEmpty()) {
                    result.addError("Hidden test " + (i + 1) + " has empty input");
                }
                if (test.getOutput() == null || test.getOutput().trim().isEmpty()) {
                    result.addError("Hidden test " + (i + 1) + " has empty output");
                }
            }
        }
    }

    /**
     * Validate skeleton code exists and is not empty
     * More advanced syntax checking could be added later
     */
    private void validateSkeletonCode(GeneratedProblemResponse problem, ValidationResult result) {
        if (problem.getSkeletonCode() == null || problem.getSkeletonCode().isEmpty()) {
            result.addWarning("No skeleton code provided");
            return;
        }

        // Check that at least Python code is provided (our validation language)
        if (!problem.getSkeletonCode().containsKey("python") ||
                problem.getSkeletonCode().get("python").trim().isEmpty()) {
            result.addWarning("No Python skeleton code provided");
        }

        // Warn if any language has empty code
        for (Map.Entry<String, String> entry : problem.getSkeletonCode().entrySet()) {
            if (entry.getValue() == null || entry.getValue().trim().isEmpty()) {
                result.addWarning("Empty skeleton code for language: " + entry.getKey());
            }
        }
    }

    /**
     * Generate a reference solution and verify it against test cases
     * This is the核心 validation that ensures the problem is actually solvable
     */
    private SolutionVerificationResult generateAndVerifySolution(GeneratedProblemResponse problem) {
        log.info("Generating reference solution for validation");

        try {
            // Step 1: Generate Python solution using LLM
            String solution = generateReferenceSolution(problem);

            // Step 2: Convert test cases to TestCase format
            List<TestCase> testCases = convertToTestCases(problem);

            // Step 3: Execute solution against all test cases
            return executeSolutionWithRateLimit(solution, testCases);

        } catch (Exception e) {
            log.error("Failed to generate/verify solution: {}", e.getMessage(), e);
            SolutionVerificationResult failedResult = new SolutionVerificationResult();
            failedResult.setAllPassed(false);
            failedResult.addFailureReason("Solution generation failed: " + e.getMessage());
            return failedResult;
        }
    }

    /**
     * Generate a reference solution using LLM
     */
    private String generateReferenceSolution(GeneratedProblemResponse problem) throws Exception {
        String prompt = buildSolutionPrompt(problem);

        // Call LLM with lower temperature for more accurate solutions
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("temperature", 0.1); // Very low for deterministic, correct solutions
        requestBody.put("max_tokens", 1500); // Increased for more complex solutions

        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content",
                "You are an expert competitive programmer. Generate correct, efficient Python solutions.");

        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);

        requestBody.put("messages", List.of(systemMessage, userMessage));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        String response = restTemplate.postForObject(apiUrl, entity, String.class);

        // Parse response
        JsonNode root = objectMapper.readTree(response);
        String solutionCode = root.get("choices").get(0)
                .get("message")
                .get("content")
                .asText();

        // Extract code from markdown if present
        solutionCode = extractCodeFromMarkdown(solutionCode);

        log.info("Generated reference solution ({} chars)", solutionCode.length());
        return solutionCode;
    }

    /**
     * Build prompt for generating reference solution
     * Uses comprehensive guidance to ensure correct, working solutions
     */
    private String buildSolutionPrompt(GeneratedProblemResponse problem) {
        StringBuilder prompt = new StringBuilder();

        // Role and context
        prompt.append("You are an expert competitive programmer solving this problem.\n\n");

        prompt.append("=== PROBLEM ===\n");
        prompt.append("Title: ").append(problem.getTitle()).append("\n\n");
        prompt.append(problem.getDescription()).append("\n\n");

        prompt.append("=== INPUT FORMAT ===\n");
        prompt.append(problem.getInputFormat()).append("\n\n");

        prompt.append("=== OUTPUT FORMAT ===\n");
        prompt.append(problem.getOutputFormat()).append("\n\n");

        prompt.append("=== CONSTRAINTS ===\n");
        for (String constraint : problem.getConstraints()) {
            prompt.append("- ").append(constraint).append("\n");
        }
        prompt.append("\n");

        // Include ALL sample tests with explanations
        prompt.append("=== SAMPLE TESTS (Study these carefully!) ===\n\n");
        for (int i = 0; i < problem.getSampleTests().size(); i++) {
            GeneratedProblemResponse.SampleTest sample = problem.getSampleTests().get(i);
            prompt.append("Sample Test ").append(i + 1).append(":\n");
            prompt.append("Input:\n").append(sample.getInput()).append("\n");
            prompt.append("Expected Output:\n").append(sample.getOutput()).append("\n");
            prompt.append("Explanation:\n").append(sample.getExplanation()).append("\n\n");
        }

        // Algorithmic approach section
        prompt.append("=== YOUR TASK ===\n\n");
        prompt.append("STEP 1: UNDERSTAND THE ALGORITHM\n");
        prompt.append("- Read the sample test explanations above CAREFULLY\n");
        prompt.append("- They show EXACTLY how to solve this problem\n");
        prompt.append("- Your solution MUST follow the same logic/steps\n");
        prompt.append(
                "- If the explanation mentions a specific technique (e.g., 'DFS', 'sliding window', 'hash map'), you MUST use it\n\n");

        prompt.append("STEP 2: PLAN YOUR SOLUTION\n");
        prompt.append("Before coding, mentally outline:\n");
        prompt.append("1. How to parse the input correctly\n");
        prompt.append("2. What data structures you need (array, hash map, tree, etc.)\n");
        prompt.append("3. The main algorithm (following the sample explanations)\n");
        prompt.append("4. How to format the output correctly\n\n");

        prompt.append("STEP 3: IMPLEMENT CORRECTLY\n");
        prompt.append("- Read from stdin using input()\n");
        prompt.append("- Parse ALL inputs as specified in INPUT FORMAT\n");
        prompt.append("- Implement the algorithm shown in sample explanations\n");
        prompt.append("- Handle edge cases from constraints\n");
        prompt.append("- Print to stdout with EXACT formatting from OUTPUT FORMAT\n\n");

        prompt.append("STEP 4: VERIFY YOUR SOLUTION\n");
        prompt.append("Mentally trace through Sample Test 1:\n");
        prompt.append("- Input: ").append(problem.getSampleTests().get(0).getInput()).append("\n");
        prompt.append("- Your code should output: ").append(problem.getSampleTests().get(0).getOutput()).append("\n");
        prompt.append("- Does your logic produce this output? If not, fix it before submitting.\n\n");

        // Code requirements
        prompt.append("=== CODE REQUIREMENTS ===\n");
        prompt.append("1. Write complete, working Python 3 code\n");
        prompt.append("2. Include NO explanatory comments (just code)\n");
        prompt.append("3. NO markdown formatting (no ```python, just raw code)\n");
        prompt.append("4. Start directly with imports or code\n");
        prompt.append("5. Use ONLY standard Python libraries (no external packages)\n");
        prompt.append("6. Handle all input/output exactly as specified\n");
        prompt.append("7. Follow the algorithm from sample explanations\n\n");

        // Edge case reminders
        prompt.append("=== CRITICAL REMINDERS ===\n");
        prompt.append("- Pay attention to 0-indexing vs 1-indexing\n");
        prompt.append("- Check if output should have newlines or spaces\n");
        prompt.append("- Handle the constraints boundaries (min/max values)\n");
        prompt.append("- Convert string inputs to int/float where needed\n");
        prompt.append("- Print output EXACTLY as shown in sample outputs\n\n");

        prompt.append("Now write your Python solution:");

        return prompt.toString();
    }

    /**
     * Extract code from markdown code blocks
     */
    private String extractCodeFromMarkdown(String content) {
        content = content.trim();

        if (content.startsWith("```")) {
            int start = content.indexOf("```");
            start = content.indexOf("\n", start) + 1;
            int end = content.indexOf("```", start);

            if (end > start) {
                return content.substring(start, end).trim();
            }
        }

        return content;
    }

    /**
     * Convert GeneratedProblemResponse test cases to TestCase entities
     */
    private List<TestCase> convertToTestCases(GeneratedProblemResponse problem) {
        List<TestCase> testCases = new ArrayList<>();

        // Add sample tests
        for (GeneratedProblemResponse.SampleTest sample : problem.getSampleTests()) {
            TestCase tc = new TestCase();
            tc.setInput(sample.getInput());
            tc.setExpectedOutput(sample.getOutput());
            tc.setType("SAMPLE");
            tc.setIsHidden(false);
            testCases.add(tc);
        }

        // Add hidden tests
        for (GeneratedProblemResponse.HiddenTest hidden : problem.getHiddenTests()) {
            TestCase tc = new TestCase();
            tc.setInput(hidden.getInput());
            tc.setExpectedOutput(hidden.getOutput());
            tc.setType("HIDDEN");
            tc.setIsHidden(true);
            testCases.add(tc);
        }

        return testCases;
    }

    /**
     * Execute solution against test cases with rate limit delays
     */
    private SolutionVerificationResult executeSolutionWithRateLimit(String solution, List<TestCase> testCases) {
        SolutionVerificationResult result = new SolutionVerificationResult();
        result.setGeneratedSolution(solution);
        result.setLanguage("python");
        result.setTotalTests(testCases.size());

        long startTime = System.currentTimeMillis();
        int passed = 0;

        log.info("Executing solution against {} test cases", testCases.size());

        for (int i = 0; i < testCases.size(); i++) {
            TestCase tc = testCases.get(i);

            try {
                // Execute with code execution service
                var executionResult = codeExecutionService.executeCode(solution, "python", tc.getInput());

                String actualOutput = executionResult.getCleanOutput();
                String expectedOutput = tc.getExpectedOutput().trim();

                if (actualOutput.equals(expectedOutput)) {
                    passed++;
                    log.debug("Test case {} passed", i + 1);
                } else {
                    String error = String.format("Test %d failed: Expected '%s', got '%s'",
                            i + 1, expectedOutput, actualOutput);
                    result.addFailureReason(error);
                    log.warn(error);
                }

                // Respect rate limit: wait between requests
                if (i < testCases.size() - 1) {
                    Thread.sleep(validationConfig.getPistonRateLimitMs());
                }

            } catch (Exception e) {
                String error = String.format("Test %d error: %s", i + 1, e.getMessage());
                result.addFailureReason(error);
                log.error(error, e);
            }
        }

        result.setTestsPassed(passed);
        result.setAllPassed(passed == testCases.size());
        result.setExecutionTimeMs(System.currentTimeMillis() - startTime);

        log.info("Solution verification complete: {}/{} tests passed", passed, testCases.size());

        return result;
    }

    /**
     * Calculate quality score based on validation results
     * Score is from 0-100
     */
    private double calculateQualityScore(ValidationResult result) {
        double score = 100.0;

        // Deduct for structural issues
        score -= result.getErrors().size() * 20.0;
        score -= result.getWarnings().size() * 5.0;

        // Solution verification bonus/penalty
        if (result.getSolutionResult() != null) {
            if (result.getSolutionResult().isValid()) {
                score += 10.0; // Bonus for verified solution
            } else {
                double passRate = result.getSolutionResult().getPassPercentage();
                score -= (100.0 - passRate) * 0.5; // Penalty based on failure rate
            }
        }

        // Ensure score is in valid range
        return Math.max(0.0, Math.min(100.0, score));
    }

    /**
     * Create a skipped validation result (when validation is disabled)
     */
    private ValidationResult createSkippedResult() {
        ValidationResult result = new ValidationResult();
        result.setValid(true);
        result.setQualityScore(0.0);
        result.addWarning("Validation was skipped (disabled in configuration)");
        return result;
    }

    /**
     * Get validation configuration
     * Used by other services to check validation settings
     */
    public ValidationConfig getValidationConfig() {
        return validationConfig;
    }
}
