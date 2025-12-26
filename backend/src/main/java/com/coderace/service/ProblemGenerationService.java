package com.coderace.service;

import com.coderace.dto.GeneratedProblemResponse;
import com.coderace.dto.GenerateTitlesResponse;
import com.coderace.dto.ProblemFilter;
import com.coderace.dto.ProblemTitleOption;
import com.coderace.dto.TitleGenerationResponse;
import com.coderace.dto.ValidationResult;
import com.coderace.config.ValidationConfig;
import com.coderace.model.Problem;
import com.coderace.model.TestCase;
import com.coderace.repository.TestCaseRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import com.coderace.constants.GameConstants;

/**
 * Service for generating coding problems using Google Gemini API
 */
@Service
@Slf4j
public class ProblemGenerationService {

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.url}")
    private String apiUrl;

    @Value("${openai.model}")
    private String model;

    @Value("${openai.temperature:0.7}")
    private double temperature;

    @Value("${openai.max.tokens:4000}")
    private int maxTokens;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final TestCaseRepository testCaseRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ProblemValidationService validationService;

    public ProblemGenerationService(RestTemplate restTemplate, ObjectMapper objectMapper,
            TestCaseRepository testCaseRepository, SimpMessagingTemplate messagingTemplate,
            ProblemValidationService validationService) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.testCaseRepository = testCaseRepository;
        this.messagingTemplate = messagingTemplate;
        this.validationService = validationService;
    }

    /**
     * Step 1: Generate 3 problem title options for user to choose from
     * Part of two-step generation flow
     *
     * @param filter Problem category, difficulty, and optional subtype
     * @param roomId Room ID for status broadcasting
     * @return 3 problem title options
     */
    @Transactional
    public GenerateTitlesResponse generateProblemTitles(ProblemFilter filter, String roomId) {
        log.info("Generating title options - Category: {}, Difficulty: {}, Subtype: {}",
                filter.category().getDisplayName(),
                filter.difficulty().getDisplayName(),
                filter.hasSubtype() ? filter.subtype() : "None");

        try {
            broadcastStatus(roomId, GameConstants.STATUS_GENERATING_OPTIONS);

            String prompt = buildTitleGenerationPrompt(filter);
            TitleGenerationResponse response = callOpenAIForTitles(prompt);

            broadcastStatus(roomId, GameConstants.STATUS_OPTIONS_READY);

            GenerateTitlesResponse result = new GenerateTitlesResponse(response.getOptions());
            log.info("Successfully generated {} title options", result.getOptions().size());
            return result;

        } catch (Exception e) {
            broadcastStatus(roomId, GameConstants.STATUS_FAILED_TITLES);
            log.error("Failed to generate title options: {}", e.getMessage(), e);
            throw new RuntimeException("Title generation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Step 2: Generate full problem from selected title
     * Now includes validation with retry logic
     * 
     * @param filter        Problem filter with category/difficulty
     * @param selectedTitle The title option user selected
     * @param roomId        Room ID for status broadcasting
     * @return Complete problem with test cases
     */
    @Transactional
    public Problem generateProblemFromTitle(ProblemFilter filter, ProblemTitleOption selectedTitle, String roomId) {
        log.info("Generating full problem from title: {}", selectedTitle.getTitle());

        ValidationConfig config = validationService.getValidationConfig();
        int maxRetries = config.isEnabled() ? config.getMaxRetries() : 0;
        int attemptNumber = 0;
        ValidationResult validation = null;

        while (attemptNumber <= maxRetries) {
            attemptNumber++;

            try {
                if (attemptNumber > 1) {
                    log.info("Regeneration attempt {} of {}", attemptNumber, maxRetries + 1);
                    broadcastStatus(roomId, "Regenerating problem (attempt " + attemptNumber + ")...");
                }

                broadcastStatus(roomId, GameConstants.STATUS_GENERATING_PROBLEM);
                String prompt = buildFullProblemPrompt(filter, selectedTitle);
                GeneratedProblemResponse response = callOpenAI(prompt);

                // Validate the generated problem
                if (config.isEnabled()) {
                    broadcastStatus(roomId, "Validating problem quality...");
                    validation = validationService.validateProblem(response, filter);

                    if (!validation.isValid()) {
                        log.warn("Problem validation failed: {}", validation.getErrors());

                        if (attemptNumber <= maxRetries) {
                            // Will retry
                            continue;
                        } else {
                            // Max retries exhausted
                            broadcastStatus(roomId, GameConstants.STATUS_FAILED_GENERATION);
                            throw new RuntimeException("Problem validation failed after " + maxRetries + " retries: " +
                                    String.join(", ", validation.getErrors()));
                        }
                    }

                    log.info("Problem validation passed with quality score: {}", validation.getQualityScore());
                }

                // Validation passed or disabled - create problem
                broadcastStatus(roomId, GameConstants.STATUS_PROCESSING_DETAILS);
                Problem problem = convertToProblem(response, filter, validation);

                broadcastStatus(roomId, GameConstants.STATUS_CREATING_TESTS);
                saveTestCases(problem, response);

                broadcastStatus(roomId, GameConstants.STATUS_PROBLEM_READY);
                log.info("Successfully generated and validated problem: {}", problem.getName());
                return problem;

            } catch (RuntimeException e) {
                if (attemptNumber > maxRetries) {
                    // This was the last attempt, rethrow
                    throw e;
                }
                // Otherwise, loop will retry
                log.warn("Attempt {} failed: {}", attemptNumber, e.getMessage());
            } catch (Exception e) {
                broadcastStatus(roomId, GameConstants.STATUS_FAILED_GENERATION);
                log.error("Failed to generate full problem: {}", e.getMessage(), e);
                throw new RuntimeException("Problem generation failed: " + e.getMessage(), e);
            }
        }

        // Should never reach here, but just in case
        broadcastStatus(roomId, GameConstants.STATUS_FAILED_GENERATION);
        throw new RuntimeException("Problem generation failed after all retry attempts");
    }

    /**
     * Original method - generates problem in one step
     * Now uses defaultFilter temporarily until frontend is updated
     *
     * @param filter Problem difficulty and tags
     * @return Generated problem with test cases
     */
    @Transactional
    public Problem generateProblem(ProblemFilter filter, String roomId) {
        log.info("Generating problem - Category: {}, Difficulty: {}, Subtype: {}",
                filter.category().getDisplayName(),
                filter.difficulty().getDisplayName(),
                filter.hasSubtype() ? filter.subtype() : "None");

        try {
            // Step 1: Building prompt
            broadcastStatus(roomId, GameConstants.STATUS_BUILDING_PROMPT);
            String prompt = buildPrompt(filter);

            // Step 2: Calling Gemini API
            broadcastStatus(roomId, GameConstants.STATUS_GENERATING_AI);
            GeneratedProblemResponse response = callOpenAI(prompt);

            // Step 3: Converting to problem
            broadcastStatus(roomId, GameConstants.STATUS_PROCESSING_DETAILS);
            Problem problem = convertToProblem(response, filter, null); // No validation in one-step generation

            // Step 4: Saving test cases
            broadcastStatus(roomId, GameConstants.STATUS_CREATING_TESTS);
            saveTestCases(problem, response);

            broadcastStatus(roomId, GameConstants.STATUS_PROBLEM_READY);
            log.info("Successfully generated problem: {}", problem.getName());
            return problem;

        } catch (Exception e) {
            broadcastStatus(roomId, GameConstants.STATUS_FAILED_GENERATION);
            log.error("Failed to generate problem: {}", e.getMessage(), e);
            throw new RuntimeException("Problem generation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Broadcast generation status to room
     */
    private void broadcastStatus(String roomId, String status) {
        Map<String, String> statusMessage = Map.of(
                "type", "GENERATION_STATUS",
                "status", status);
        messagingTemplate.convertAndSend("/topic/room/" + roomId, statusMessage);
        log.info("[Room {}] {}", roomId, status);
    }

    /**
     * Build the prompt for OpenAI API using structured filter
     */
    private String buildPrompt(ProblemFilter filter) {
        String categoryName = filter.category().getDisplayName();
        String subtypeClause = filter.hasSubtype()
                ? String.format(" specifically focusing on %s", filter.subtype())
                : "";
        int targetRating = filter.difficulty().getAverageRating();

        return String.format(
                """
                        You are an expert competitive programming problem setter. Generate a HIGH-QUALITY competitive programming problem:

                        PROBLEM REQUIREMENTS:
                        - Category: %s%s
                        - Difficulty: %s (target rating: %d)
                        - The problem MUST require %s to solve
                        - DO NOT mix in other algorithmic concepts unless the category naturally requires them
                        - Tags should ONLY include "%s" and directly related concepts

                        CRITICAL REQUIREMENTS:
                        1. The problem MUST be SOLVABLE and ALGORITHMICALLY SOUND
                        2. DO NOT ask students to simply "implement X algorithm" - instead, create a scenario that REQUIRES that algorithm
                        3. Test cases MUST NOT contradict the stated constraints
                        4. Include explicit time/space complexity requirements when relevant
                        5. All test outputs must be VERIFIED CORRECT
                        6. STRICTLY adhere to the category - do not introduce unrelated concepts

                        PROBLEM DESIGN GUIDELINES:
                        - Create a creative real-world scenario or story
                        - The problem should test UNDERSTANDING, not just memorization
                        - Add a twist or variation to make it interesting (e.g., "find FIRST occurrence" instead of just "find element")
                        - Make it challenging but not impossible for intermediate programmers

                        FORMAT SPECIFICATIONS:

                        **Problem Statement** (300-500 words):
                        - Engaging scenario with clear context
                        - Precise problem requirements
                        - Include sample walkthrough with small example

                        **Input Format**:
                        - Exact specification of each line
                        - Specify data types and order clearly

                        **Output Format**:
                        - Exact specification of output
                        - Specify formatting (e.g., space-separated, new lines)

                        **Constraints**:
                        - List ALL constraints with exact bounds
                        - Include time/space complexity if algorithm-specific (e.g., "Your solution must run in O(log n) time")

                        **Sample Test Cases** (2-3 examples):
                        - Each must include: input, output, AND step-by-step explanation
                        - Explanation should walk through the algorithm/logic
                        - Cover different scenarios (normal case, edge case, etc.)

                        **Hidden Test Cases** (exactly 10):
                        - Test 1-2: Basic/normal cases
                        - Test 3-5: Edge cases (but MUST comply with constraints!)
                          * If constraints say n≥1, do NOT use n=0
                          * Test minimum and maximum bounds AS STATED in constraints
                        - Test 6-8: Corner cases (e.g., all same elements, alternating pattern)
                        - Test 9-10: Random valid cases

                        VERIFICATION CHECKLIST (CRITICAL - verify EVERY item before outputting):

                        === MATHEMATICAL CORRECTNESS (MANUALLY VERIFY) ===
                        ✓ MANUALLY calculate expected output for EVERY sample test
                        ✓ VERIFY arithmetic in explanations matches actual calculation
                        ✓ DOUBLE-CHECK array indices, lengths, counts are EXACT
                        ✓ RE-CHECK terminology ("subarray sum" vs "prefix sum", etc.)
                        ✓ ENSURE output values match your hand-calculated results

                        === STANDARD CHECKS ===
                        ✓ All test inputs satisfy the stated constraints
                        ✓ All test outputs are algorithmically correct
                        ✓ Problem requires %s as specified
                        ✓ Problem is not just "implement %s"
                        ✓ Time/space complexity stated if relevant
                        ✓ Sample explanations walk through the solution
                        ✓ NO unrelated algorithmic concepts mixed in

                        Return ONLY valid JSON (no markdown code fences):
                        {
                          "title": "Creative, engaging title",
                          "description": "Full problem statement with HTML formatting (<p>, <strong>, <code>, etc.)",
                          "inputFormat": "Precise input specification",
                          "outputFormat": "Precise output specification",
                          "constraints": [
                            "1 ≤ n ≤ 100000",
                            "Time complexity: O(log n) required",
                            "..."
                          ],
                          "sampleTests": [
                            {
                              "input": "exact input",
                              "output": "exact output",
                              "explanation": "Step-by-step walkthrough of solution"
                            }
                          ],
                          "hiddenTests": [
                            {"input": "test input", "output": "correct output"}
                          ],
                          "difficulty": %d,
                          "tags": ["%s"],
                          "skeletonCode": {
                            "python": "# Starter code matching inputFormat/outputFormat with TODO comment",
                            "java": "// Starter code matching inputFormat/outputFormat with TODO comment",
                            "cpp": "// Starter code matching inputFormat/outputFormat with TODO comment",
                            "javascript": "// Starter code matching inputFormat/outputFormat with TODO comment"
                          }
                        }
                        """,
                categoryName,
                subtypeClause,
                filter.difficulty().getDisplayName(),
                targetRating,
                categoryName,
                categoryName.toLowerCase(),
                categoryName,
                categoryName,
                targetRating,
                categoryName.toLowerCase());
    }

    /**
     * Build prompt for generating 3 title options
     */
    private String buildTitleGenerationPrompt(ProblemFilter filter) {
        String categoryName = filter.category().getDisplayName();
        String subtypeClause = filter.hasSubtype()
                ? String.format(" focusing on %s", filter.subtype())
                : "";

        return String.format(
                """
                        Generate exactly 3 creative competitive programming problem titles:

                        Category: %s%s
                        Difficulty: %s

                        Requirements:
                        - Each problem MUST require %s to solve
                        - Titles should be creative and engaging (not generic like "Problem 1")
                        - Include brief 1-2 sentence description
                        - Specify exact concept/pattern needed
                        - DO NOT mix in unrelated algorithmic concepts

                        Return ONLY valid JSON (no markdown code fences):
                        {
                          "options": [
                            {
                              "title": "The Museum Heist",
                              "briefDescription": "Maximize treasure collected while respecting weight limits and room constraints.",
                              "concept": "0/1 Knapsack variant"
                            },
                            {
                              "title": "Resource Allocation Game",
                              "briefDescription": "Distribute limited resources optimally across multiple projects.",
                              "concept": "Bounded Knapsack"
                            },
                            {
                              "title": "The Treasure Collector",
                              "briefDescription": "Navigate a maze collecting valuable items with capacity constraints.",
                              "concept": "Multi-dimensional DP"
                            }
                          ]
                        }
                        """,
                categoryName,
                subtypeClause,
                filter.difficulty().getDisplayName(),
                categoryName);
    }

    /**
     * Call OpenAI API specifically for title generation
     */
    private TitleGenerationResponse callOpenAIForTitles(String prompt) {
        try {
            // Build request payload
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("temperature", 0.8); // Slightly higher for creativity
            requestBody.put("max_tokens", 500); // Less tokens needed for titles

            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content",
                    "You are a competitive programming problem generator. Generate creative problem titles in valid JSON format.");

            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);

            requestBody.put("messages", List.of(systemMessage, userMessage));

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            // Make request
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            String response = restTemplate.postForObject(apiUrl, entity, String.class);

            // Parse response
            JsonNode root = objectMapper.readTree(response);
            String jsonContent = root.get("choices").get(0)
                    .get("message")
                    .get("content")
                    .asText();

            // Extract JSON from markdown if present
            jsonContent = extractJsonFromMarkdown(jsonContent);

            // Parse title response
            TitleGenerationResponse titleResponse = objectMapper.readValue(jsonContent,
                    TitleGenerationResponse.class);

            log.info("Successfully generated {} title options", titleResponse.getOptions().size());
            return titleResponse;

        } catch (Exception e) {
            log.error("Error calling OpenAI API for titles: {}", e.getMessage());
            throw new RuntimeException("Title generation API call failed", e);
        }
    }

    /**
     * Build prompt for full problem generation from selected title
     * Uses test-first, educational approach for better quality
     */
    private String buildFullProblemPrompt(ProblemFilter filter, ProblemTitleOption selectedTitle) {
        String categoryName = filter.category().getDisplayName();
        int targetRating = filter.difficulty().getAverageRating();

        // Include subtype constraint if specified
        String subtypeRequirement = filter.hasSubtype()
                ? String.format("\n7. Problem MUST specifically use %s pattern/technique", filter.subtype())
                : "";

        String subtypeEmphasis = filter.hasSubtype()
                ? String.format(" The problem MUST use the %s technique specifically.", filter.subtype())
                : "";

        return String.format(
                """
                        You are an expert competitive programming problem author creating educational, solvable problems.

                        === PROBLEM TO CREATE ===
                        Title: %s
                        Brief: %s
                        Required Algorithm: %s
                        Category: %s
                        Difficulty: %s (target rating: %d)
                        %s

                        === YOUR APPROACH (Follow this order!) ===

                        STEP 1: DESIGN THE ALGORITHM FIRST
                        Before writing anything else, think through:
                        - What is the core algorithm/technique required (%s)?
                        - What are the key steps someone would take to solve this?
                        - What data structures are needed?
                        - What's the time/space complexity?

                        STEP 2: CREATE SAMPLE TEST CASES (Most Important!)
                        Create 2-3 sample tests where you:
                        1. Choose simple, clear inputs
                        2. MANUALLY calculate the outputs step-by-step
                        3. Write a DETAILED explanation showing:
                           - Initial state
                           - Each step of the algorithm
                           - The reasoning/logic at each step
                           - The final answer

                        CRITICAL: These explanations must be SO CLEAR that someone could implement
                        the solution just by reading them. They will be used to generate reference
                        solutions, so include ALL necessary details!

                        Example of a GOOD explanation:
                        "Input: [2, 7, 11, 15], target = 9

                         Step 1: Initialize empty hash map {}
                         Step 2: Iterate through array
                           - index 0: num=2, complement=9-2=7
                           - 7 not in map, add {2: 0}
                           - index 1: num=7, complement=9-7=2
                           - 2 IS in map at index 0
                           - Return [0, 1]

                         Output: [0, 1]"

                        Example of BAD explanation (too vague):
                        "Use a hash map to find the two numbers that sum to target."

                        STEP 3: CREATE HIDDEN TEST CASES
                        Generate exactly 10 hidden tests:
                        - Tests 1-2: Simple, basic cases
                        - Tests 3-5: Edge cases (minimum/maximum values from constraints)
                        - Tests 6-8: Corner cases (empty, single element, all same, etc.)
                        - Tests 9-10: Random valid cases

                        For EACH test, manually calculate the correct output before adding it!

                        STEP 4: WRITE THE PROBLEM DESCRIPTION
                        Based on the algorithm and test cases above:
                        - Create an engaging narrative/scenario
                        - Clearly state the objective
                        - Define input/output formats precisely
                        - List all constraints with exact bounds
                        - Walk through one sample test in the description

                        === STRICT REQUIREMENTS ===

                        1. ALGORITHM FOCUS:
                           - Problem MUST require %s (%s) to solve%s
                           - DO NOT mix in other unrelated algorithmic concepts
                           - The problem should APPLY the algorithm, not just ask to implement it

                        2. CLARITY & PRECISION:
                           - Input format: Specify EXACT number of lines, data types, order
                           - Output format: Specify EXACT formatting (spaces, newlines, etc.)
                           - Constraints: Include ALL bounds (e.g., "1 ≤ n ≤ 10^5", not just "small n")
                           - Terminology: Use precise terms (e.g., "subarray" vs "subsequence")

                        3. TEST CASE CORRECTNESS:
                           - ALL test inputs must satisfy the stated constraints
                           - ALL test outputs must be mathematically/algorithmically correct
                           - Sample explanations must match the actual calculations
                           - NO contradictions between constraints and tests

                        4. EDUCATIONAL VALUE:
                           - Sample explanations must teach the algorithm (very detailed!)
                           - Include hints about approach in the problem description
                           - State time/space complexity requirements if relevant
                           - Progressive difficulty in test cases (easy → hard)

                        5. SOLVABILITY:
                           - The problem must be solvable within 30-60 minutes
                           - Solution should be implementable in 50-100 lines of code
                           - No ambiguity in problem statement
                           - Test cases should cover all edge cases mentioned in constraints

                        === FORMAT SPECIFICATIONS ===

                        **Problem Description** (300-500 words):
                        - Engaging real-world scenario or narrative
                        - Clear problem objective
                        - Precise requirements
                        - Sample walkthrough with explanation
                        - Hints about the approach (without giving it away)

                        **Input Format**:
                        - Line-by-line specification
                        - Data types for each input
                        - Order of inputs
                        - Example: "First line: integer n (1 ≤ n ≤ 1000)"

                        **Output Format**:
                        - Exact output specification
                        - Formatting details (space-separated, one per line, etc.)
                        - Precision for floating point if applicable

                        **Constraints**:
                        - ALL constraints with exact numerical bounds
                        - Time complexity requirement (if algorithm-specific)
                        - Space complexity requirement (if relevant)
                        - Special constraints (e.g., "all elements are distinct")

                        **Sample Tests** (2-3 examples):
                        Each MUST include:
                        - Input: Exact input string
                        - Output: Exact output string
                        - Explanation: VERY DETAILED step-by-step walkthrough showing:
                          * The algorithm steps
                          * Intermediate calculations
                          * Why each step is taken
                          * How the final answer is derived

                        **Hidden Tests** (exactly 10):
                        - Diverse coverage of input space
                        - Edge cases at constraint boundaries
                        - Corner cases (empty, single element, maximum size, etc.)
                        - All tests must be valid and have correct outputs

                        === VERIFICATION CHECKLIST ===

                        Before finalizing, verify:
                        ✓ Sample test explanations are extremely detailed (5+ sentences each)
                        ✓ Explanations show the exact algorithm steps
                        ✓ All test outputs are manually calculated and verified
                        ✓ Constraints match all test inputs
                        ✓ Input/output formats are crystal clear
                        ✓ Problem requires the specified algorithm (%s)
                        ✓ Problem tests understanding, not just implementation
                        ✓ Time/space complexity stated if relevant
                        ✓ No unrelated concepts mixed in
                        ✓ Skeleton code has correct I/O format%s

                        === SKELETON CODE ===

                        Generate starter code for Python, Java, C++, JavaScript that:
                        1. Reads input EXACTLY matching inputFormat
                        2. Has TODO comment marking where to implement algorithm
                        3. Outputs result EXACTLY matching outputFormat
                        4. Is syntactically correct and runs
                        5. Uses standard competitive programming patterns

                        CRITICAL: In JSON, escape backslashes as \\\\\\\\ (e.g., \\\\\\\\n for newline)

                        === OUTPUT FORMAT ===

                        Return ONLY valid JSON (no markdown code fences, no explanatory text):
                        {
                          "title": "%s",
                          "description": "Full problem statement (HTML allowed)",
                          "inputFormat": "Line-by-line specification with types",
                          "outputFormat": "Exact output specification",
                          "constraints": ["1 ≤ n ≤ 1000", "1 ≤ arr[i] ≤ 10^9", "time: O(n log n)"],
                          "sampleTests": [
                            {
                              "input": "exact input string",
                              "output": "exact output string",
                              "explanation": "VERY detailed step-by-step walkthrough (minimum 5 sentences showing algorithm steps)"
                            }
                          ],
                          "hiddenTests": [
                            {"input": "test input", "output": "manually calculated output"}
                          ],
                          "difficulty": %d,
                          "tags": ["%s"],
                          "skeletonCode": {
                            "python": "# Starter code with correct I/O\\\\n",
                            "java": "import java.util.*;\\\\n\\\\npublic class Solution { ... }",
                            "cpp": "#include <iostream>\\\\nusing namespace std;\\\\n\\\\nint main() { ... }",
                            "javascript": "const readline = require('readline');\\\\n..."
                          }
                        }
                        """,
                selectedTitle.getTitle(),
                selectedTitle.getBriefDescription(),
                selectedTitle.getConcept(),
                categoryName,
                filter.difficulty().getDisplayName(),
                targetRating,
                filter.hasSubtype() ? String.format("Specific Technique: %s", filter.subtype()) : "",
                selectedTitle.getConcept(),
                categoryName,
                selectedTitle.getConcept(),
                subtypeEmphasis,
                categoryName, // For verification
                subtypeRequirement,
                selectedTitle.getTitle(),
                targetRating,
                categoryName.toLowerCase());
    }

    /**
     * Call OpenAI API with the prompt
     */
    private GeneratedProblemResponse callOpenAI(String prompt) {
        try {
            // Build request payload for OpenAI Chat Completion
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("temperature", temperature);
            requestBody.put("max_tokens", maxTokens);

            // Messages array
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content",
                    "You are a competitive programming problem generator. Generate problems in valid JSON format without markdown code fences.");

            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);

            requestBody.put("messages", List.of(systemMessage, userMessage));

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            // Make request
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            String response = restTemplate.postForObject(apiUrl, entity, String.class);

            // Parse response
            JsonNode root = objectMapper.readTree(response);
            log.info("OpenAI response received");

            JsonNode choicesNode = root.get("choices");
            if (choicesNode == null || choicesNode.isEmpty()) {
                throw new RuntimeException("No response from OpenAI API");
            }

            String jsonContent = choicesNode.get(0)
                    .get("message")
                    .get("content")
                    .asText();

            log.info("OpenAI content length: {}", jsonContent.length());

            // Extract JSON from markdown code blocks if present
            jsonContent = extractJsonFromMarkdown(jsonContent);

            // Parse problem response
            GeneratedProblemResponse problemResponse = objectMapper.readValue(jsonContent,
                    GeneratedProblemResponse.class);

            log.info("Successfully generated problem: {}", problemResponse.getTitle());
            return problemResponse;

        } catch (Exception e) {
            log.error("Error calling OpenAI API: {}", e.getMessage());
            throw new RuntimeException("OpenAI API call failed", e);
        }
    }

    /**
     * Extract JSON from markdown code blocks
     */
    private String extractJsonFromMarkdown(String content) {
        log.info("EXTRACTION DEBUG - Input length: {}, starts with: '{}'",
                content.length(),
                content.substring(0, Math.min(50, content.length())));

        // Trim content first
        content = content.trim();

        log.info("EXTRACTION DEBUG - After trim, starts with: '{}'",
                content.substring(0, Math.min(50, content.length())));

        // Remove markdown code blocks if present
        if (content.startsWith("```")) {
            log.info("EXTRACTION DEBUG - Found markdown code fence, extracting JSON...");

            // Find the start of JSON (after ```json or just ```)
            int start = content.indexOf("```");
            start = content.indexOf("\n", start) + 1; // Move to next line after ```

            // Find the ending ```
            int end = content.indexOf("```", start);

            if (end > start) {
                String extracted = content.substring(start, end).trim();
                log.info("EXTRACTION DEBUG - Successfully extracted JSON, length: {}, starts with: '{}'",
                        extracted.length(),
                        extracted.substring(0, Math.min(50, extracted.length())));
                return extracted;
            } else {
                log.warn("EXTRACTION DEBUG - Could not find ending code fence!");
            }
        } else {
            log.info("EXTRACTION DEBUG - No markdown code fence found, returning as-is");
        }

        return content;
    }

    /**
     * Convert Gemini response to Problem entity
     * Now includes validation metadata
     */
    private Problem convertToProblem(GeneratedProblemResponse response, ProblemFilter filter,
            ValidationResult validation) {
        Problem problem = new Problem();

        // Generate unique problem ID
        problem.setProblemId("GEN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        problem.setName(response.getTitle());
        problem.setRating(response.getDifficulty());
        problem.setTags(response.getTags());

        // Build full description with input/output format
        String fullDescription = buildDescription(response);
        problem.setDescription(fullDescription);

        // LLM metadata
        problem.setSource("GENERATED");
        problem.setLlmModel("gpt-4o-mini");
        problem.setGeneratedAt(LocalDateTime.now());
        problem.setIsVerified(false);

        // Validation metadata (if validation was performed)
        if (validation != null) {
            problem.setIsValidated(true);
            problem.setValidationStatus(validation.isValid() ? com.coderace.model.ValidationStatus.PASSED
                    : com.coderace.model.ValidationStatus.FAILED);
            problem.setQualityScore((int) Math.round(validation.getQualityScore()));
            problem.setValidatedAt(LocalDateTime.now());

            // Store validation errors as JSON if any exist
            if (validation.hasErrors() || validation.hasWarnings()) {
                try {
                    Map<String, Object> errorData = Map.of(
                            "errors", validation.getErrors(),
                            "warnings", validation.getWarnings());
                    problem.setValidationErrors(objectMapper.writeValueAsString(errorData));
                } catch (Exception e) {
                    log.warn("Failed to serialize validation errors: {}", e.getMessage());
                }
            }
        } else {
            problem.setIsValidated(false);
            problem.setValidationStatus(com.coderace.model.ValidationStatus.SKIPPED);
        }

        // Sample test case for display
        if (!response.getSampleTests().isEmpty()) {
            GeneratedProblemResponse.SampleTest firstSample = response.getSampleTests().get(0);
            problem.setSampleInput(firstSample.getInput());
            problem.setSampleOutput(firstSample.getOutput());
        }

        // Skeleton code for different languages
        problem.setSkeletonCode(response.getSkeletonCode());

        // Debug: Log skeleton code availability
        if (response.getSkeletonCode() != null && !response.getSkeletonCode().isEmpty()) {
            log.info("Skeleton code generated for {} languages: {}",
                    response.getSkeletonCode().size(),
                    response.getSkeletonCode().keySet());
        } else {
            log.warn("No skeleton code generated for problem: {}", problem.getName());
        }

        return problem;
    }

    /**
     * Build full problem description HTML
     */
    private String buildDescription(GeneratedProblemResponse response) {
        StringBuilder html = new StringBuilder();

        html.append("<div class='problem-description'>");
        html.append(response.getDescription());
        html.append("</div>");

        // Only add input/output format sections if they're provided and not null
        if (response.getInputFormat() != null && !response.getInputFormat().trim().isEmpty()) {
            html.append("<div class='problem-input-output'>");
            html.append("<h3>Input Format</h3>");
            html.append("<p>").append(response.getInputFormat()).append("</p>");
            html.append("</div>");
        }

        if (response.getOutputFormat() != null && !response.getOutputFormat().trim().isEmpty()) {
            html.append("<div class='problem-input-output'>");
            html.append("<h3>Output Format</h3>");
            html.append("<p>").append(response.getOutputFormat()).append("</p>");
            html.append("</div>");
        }

        html.append("<div class='problem-constraints'>'");
        html.append("<h3>Constraints</h3>");
        html.append("<ul>");
        for (String constraint : response.getConstraints()) {
            html.append("<li>").append(constraint).append("</li>");
        }
        html.append("</ul>");
        html.append("</div>");

        html.append("<div class='problem-examples'>");
        html.append("<h3>Sample Test Cases</h3>");
        for (int i = 0; i < response.getSampleTests().size(); i++) {
            GeneratedProblemResponse.SampleTest test = response.getSampleTests().get(i);
            html.append("<div class='example'>");
            html.append("<p><strong>Example ").append(i + 1).append(":</strong></p>");
            html.append("<pre><strong>Input:</strong>\n").append(test.getInput()).append("</pre>");
            html.append("<pre><strong>Output:</strong>\n").append(test.getOutput()).append("</pre>");
            html.append("<p><em>Explanation:</em> ").append(test.getExplanation()).append("</p>");
            html.append("</div>");
        }
        html.append("</div>");

        return html.toString();
    }

    /**
     * Save test cases to database
     */
    private void saveTestCases(Problem problem, GeneratedProblemResponse response) {
        List<TestCase> testCases = new ArrayList<>();

        // Add sample test cases (visible)
        for (GeneratedProblemResponse.SampleTest sample : response.getSampleTests()) {
            TestCase tc = new TestCase();
            tc.setProblemId(problem.getProblemId());
            tc.setInput(sample.getInput());
            tc.setExpectedOutput(sample.getOutput());
            tc.setType("SAMPLE");
            tc.setIsHidden(false);
            testCases.add(tc);
        }

        // Add hidden test cases
        for (GeneratedProblemResponse.HiddenTest hidden : response.getHiddenTests()) {
            TestCase tc = new TestCase();
            tc.setProblemId(problem.getProblemId());
            tc.setInput(hidden.getInput());
            tc.setExpectedOutput(hidden.getOutput());
            tc.setType("GENERATED");
            tc.setIsHidden(true);
            testCases.add(tc);
        }

        testCaseRepository.saveAll(testCases);
        log.info("Saved {} test cases for problem {}", testCases.size(), problem.getProblemId());
    }

    /**
     * Get difficulty level label
     */
    private String getDifficultyLevel(int rating) {
        if (rating < 1000)
            return "Easy";
        if (rating < 1400)
            return "Medium";
        if (rating < 1800)
            return "Hard";
        return "Expert";
    }
}
