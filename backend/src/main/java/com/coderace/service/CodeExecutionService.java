package com.coderace.service;

import com.coderace.dto.ExecutionResult;
import com.coderace.dto.SubmissionVerdict;
import com.coderace.model.TestCase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Service for executing code using Piston API
 * Piston is a free code execution engine that supports 40+ languages
 */
@Service
@Slf4j
public class CodeExecutionService {

    private static final String PISTON_API_URL = "https://emkc.org/api/v2/piston/execute";
    private static final int MAX_CODE_LENGTH = 10000; // 10KB
    private static final int EXECUTION_TIMEOUT = 5000; // 5 seconds

    // Piston API rate limit: 1 request per 200ms
    // We use 250ms to be safe
    private static final int PISTON_RATE_LIMIT_MS = 250;

    // Whitelist of allowed languages for security
    private static final Set<String> ALLOWED_LANGUAGES = Set.of(
            "python", "java", "cpp", "javascript", "c", "go", "rust");

    // Language version mappings
    private static final Map<String, String> LANGUAGE_VERSIONS = Map.of(
            "python", "3.10.0",
            "java", "15.0.2",
            "cpp", "10.2.0",
            "c", "10.2.0",
            "javascript", "16.3.0",
            "go", "1.16.2",
            "rust", "1.68.2");

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public CodeExecutionService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Executes code against a single test case
     * 
     * @param code     The source code
     * @param language Programming language
     * @param input    Test input
     * @return Execution result
     */
    public ExecutionResult executeCode(String code, String language, String input) {
        // Validation
        validateInput(code, language);

        try {
            // Build request payload
            Map<String, Object> request = new HashMap<>();
            request.put("language", language);
            request.put("version", getLanguageVersion(language));
            request.put("files", List.of(Map.of("content", code)));
            request.put("stdin", input);

            log.debug("Executing code with Piston API: language={}", language);

            // Call Piston API
            String response = restTemplate.postForObject(PISTON_API_URL, request, String.class);
            JsonNode root = objectMapper.readTree(response);

            // Parse response
            JsonNode run = root.get("run");
            String output = run.get("stdout").asText();
            String error = run.get("stderr").asText();
            int exitCode = run.get("code").asInt();

            log.debug("Execution completed: exitCode={}, output={} chars", exitCode, output.length());

            return new ExecutionResult(output, error, exitCode, 0);

        } catch (Exception e) {
            log.error("Error executing code: {}", e.getMessage());
            return new ExecutionResult("", "Execution error: " + e.getMessage(), 1, 0);
        }
    }

    /**
     * Verifies a submission against all test cases
     * 
     * @param code      Source code
     * @param language  Programming language
     * @param testCases List of test cases to run
     * @return Submission verdict with results
     */
    public SubmissionVerdict verifySubmission(String code, String language, List<TestCase> testCases) {
        validateInput(code, language);

        if (testCases == null || testCases.isEmpty()) {
            return new SubmissionVerdict(0, 0, List.of("No test cases available"));
        }

        int passed = 0;
        List<String> errors = new ArrayList<>();
        String firstFailedTest = null;

        log.info("Verifying submission against {} test cases", testCases.size());

        for (int i = 0; i < testCases.size(); i++) {
            TestCase tc = testCases.get(i);

            try {
                ExecutionResult result = executeCode(code, language, tc.getInput());

                if (!result.isSuccess()) {
                    String error = String.format("Test %d: Runtime error - %s", i + 1, result.getError());
                    errors.add(error);
                    if (firstFailedTest == null) {
                        firstFailedTest = tc.getInput();
                    }
                    log.warn(error);
                    break; // Stop on first runtime error
                }

                String actualOutput = result.getCleanOutput();
                String expectedOutput = tc.getExpectedOutput().trim();

                if (actualOutput.equals(expectedOutput)) {
                    passed++;
                    log.debug("Test {} passed", i + 1);
                } else {
                    String error = String.format("Test %d: Wrong answer. Expected '%s', got '%s'",
                            i + 1, expectedOutput, actualOutput);
                    errors.add(error);
                    if (firstFailedTest == null) {
                        firstFailedTest = tc.getInput();
                    }
                    log.info(error);
                    break; // Stop on first wrong answer
                }

                // Rate limiting: Wait between test cases to respect Piston API limits
                // Skip delay after the last test case or if we're about to break
                if (i < testCases.size() - 1) {
                    try {
                        Thread.sleep(PISTON_RATE_LIMIT_MS);
                        log.debug("Rate limit delay: {}ms before next test case", PISTON_RATE_LIMIT_MS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.warn("Rate limit delay interrupted");
                        break;
                    }
                }

            } catch (Exception e) {
                String error = String.format("Test %d: Exception - %s", i + 1, e.getMessage());
                errors.add(error);
                if (firstFailedTest == null) {
                    firstFailedTest = tc.getInput();
                }
                log.error(error, e);
                break;
            }
        }

        SubmissionVerdict verdict = new SubmissionVerdict(passed, testCases.size(), errors);
        verdict.setFirstFailedTest(firstFailedTest);

        log.info("Verification complete: verdict={}, passed={}/{}",
                verdict.getVerdict(), passed, testCases.size());

        return verdict;
    }

    /**
     * Validates code and language input
     */
    private void validateInput(String code, String language) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Code cannot be empty");
        }

        if (code.length() > MAX_CODE_LENGTH) {
            throw new IllegalArgumentException("Code too long (max " + MAX_CODE_LENGTH + " chars)");
        }

        if (!ALLOWED_LANGUAGES.contains(language)) {
            throw new IllegalArgumentException("Language not supported: " + language);
        }
    }

    /**
     * Gets the version string for a language
     */
    private String getLanguageVersion(String language) {
        return LANGUAGE_VERSIONS.getOrDefault(language, "*");
    }

    /**
     * Gets list of supported languages
     */
    public Set<String> getSupportedLanguages() {
        return ALLOWED_LANGUAGES;
    }
}
