package com.coderace.service;

import com.coderace.dto.GeneratedProblemResponse;
import com.coderace.dto.GenerateTitlesResponse;
import com.coderace.dto.ProblemFilter;
import com.coderace.dto.ProblemTitleOption;
import com.coderace.dto.TitleGenerationResponse;
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

    public ProblemGenerationService(RestTemplate restTemplate, ObjectMapper objectMapper,
            TestCaseRepository testCaseRepository, SimpMessagingTemplate messagingTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.testCaseRepository = testCaseRepository;
        this.messagingTemplate = messagingTemplate;
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
     * 
     * @param filter        Problem filter with category/difficulty
     * @param selectedTitle The title option user selected
     * @param roomId        Room ID for status broadcasting
     * @return Complete problem with test cases
     */
    @Transactional
    public Problem generateProblemFromTitle(ProblemFilter filter, ProblemTitleOption selectedTitle, String roomId) {
        log.info("Generating full problem from title: {}", selectedTitle.getTitle());

        try {
            broadcastStatus(roomId, GameConstants.STATUS_GENERATING_PROBLEM);

            String prompt = buildFullProblemPrompt(filter, selectedTitle);
            GeneratedProblemResponse response = callOpenAI(prompt);

            broadcastStatus(roomId, GameConstants.STATUS_PROCESSING_DETAILS);
            Problem problem = convertToProblem(response, filter);

            broadcastStatus(roomId, GameConstants.STATUS_CREATING_TESTS);
            saveTestCases(problem, response);

            broadcastStatus(roomId, GameConstants.STATUS_PROBLEM_READY);
            log.info("Successfully generated full problem: {}", problem.getName());
            return problem;

        } catch (Exception e) {
            broadcastStatus(roomId, GameConstants.STATUS_FAILED_GENERATION);
            log.error("Failed to generate full problem: {}", e.getMessage(), e);
            throw new RuntimeException("Problem generation failed: " + e.getMessage(), e);
        }
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
            Problem problem = convertToProblem(response, filter);

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
                        Generate a complete competitive programming problem with:

                        Title: %s
                        Brief Description: %s
                        Required Concept: %s
                        Category: %s
                        Difficulty: %s (target rating: %d)
                        %s

                        STRICT REQUIREMENTS:
                        1. Problem MUST require %s (%s) to solve%s
                        2. DO NOT mix in other unrelated concepts
                        3. Tags should ONLY include: ["%s" and directly related concepts]
                        4. Difficulty rating should be: %d
                        5. The problem description should expand on the brief description provided
                        6. All test cases must be solvable using the specified concept%s

                        CRITICAL REQUIREMENTS:
                        1. The problem MUST be SOLVABLE and ALGORITHMICALLY SOUND
                        2. Create a scenario that REQUIRES the algorithm (not just "implement X")
                        3. Test cases MUST NOT contradict the stated constraints
                        4. Include explicit time/space complexity requirements
                        5. All test outputs must be VERIFIED CORRECT

                        PROBLEM DESIGN GUIDELINES:
                        - Create a creative real-world scenario or story
                        - The problem should test UNDERSTANDING, not just memorization
                        - Add a twist or variation to make it interesting
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
                        - Include time/space complexity if algorithm-specific

                        **Sample Test Cases** (2-3 examples):
                        - Each must include: input, output, AND step-by-step explanation
                        - Explanation should walk through the algorithm/logic
                        - Cover different scenarios (normal case, edge case, etc.)

                        **Hidden Test Cases** (exactly 10):
                        - Test 1-2: Basic/normal cases
                        - Test 3-5: Edge cases (MUST comply with constraints!)
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
                        ✓ Skeleton code compiles/parses without errors for ALL languages
                        ✓ Skeleton code reads input EXACTLY as specified in inputFormat
                        ✓ Skeleton code outputs EXACTLY as specified in outputFormat
                        ✓ Skeleton code has clear TODO comment marking algorithm implementation

                        **Skeleton Code** (CRITICAL - Generate for 4 languages):

                        For Python, Java, C++, and JavaScript, generate starter code that:
                        1. Reads input EXACTLY matching the inputFormat
                        2. Has a clearly marked function/section for algorithm implementation (with TODO comment)
                        3. Outputs result EXACTLY matching the outputFormat
                        4. Compiles/runs without syntax errors
                        5. Works with the sample test cases (even if logic returns wrong answer)

                        SKELETON CODE REQUIREMENTS:
                        - Must be syntactically correct and executable
                        - Must match the EXACT input format (number of inputs, data types, order)
                        - Must match the EXACT output format (formatting, delimiters)
                        - Must have TODO comment showing where to implement the algorithm
                        - Must NOT implement the actual solution (leave logic as placeholder)
                        - Use standard competitive programming patterns (Scanner for Java, cin for C++, etc.)
                        - CRITICAL: In JSON strings, ALL backslashes must be escaped as \\\\ (e.g., \\\\n for newline, \\\\\\\\ for single backslash)
                        - CRITICAL: Do NOT use regex patterns or escape sequences that aren't valid in JSON (only valid: \\\", \\\\, \\/, \\b, \\f, \\n, \\r, \\t)

                        Return ONLY valid JSON (no markdown code fences):
                        {
                          "title": "%s",
                          "description": "Full problem statement with HTML formatting",
                          "inputFormat": "Precise input specification",
                          "outputFormat": "Precise output specification",
                          "constraints": ["constraint1", "constraint2"],
                          "sampleTests": [
                            {
                              "input": "exact input",
                              "output": "exact output",
                              "explanation": "Step-by-step walkthrough"
                            }
                          ],
                          "hiddenTests": [
                            {"input": "test input", "output": "correct output"}
                          ],
                          "difficulty": %d,
                          "tags": ["%s"],
                          "skeletonCode": {
                            "python": "# Python starter code\\nn = int(input())\\n\\n# TODO: Implement your algorithm here\\nresult = 0\\n\\nprint(result)",
                            "java": "import java.util.*;\\n\\npublic class Solution {\\n    public static void main(String[] args) {\\n        Scanner sc = new Scanner(System.in);\\n        int n = sc.nextInt();\\n        \\n        // TODO: Implement your algorithm here\\n        int result = 0;\\n        \\n        System.out.println(result);\\n    }\\n}",
                            "cpp": "#include <iostream>\\nusing namespace std;\\n\\nint main() {\\n    int n;\\n    cin >> n;\\n    \\n    // TODO: Implement your algorithm here\\n    int result = 0;\\n    \\n    cout << result << endl;\\n    return 0;\\n}",
                            "javascript": "const readline = require('readline');\\nconst rl = readline.createInterface({\\n    input: process.stdin,\\n    output: process.stdout\\n});\\n\\nrl.on('line', (line) => {\\n    const n = parseInt(line);\\n    \\n    // TODO: Implement your algorithm here\\n    const result = 0;\\n    \\n    console.log(result);\\n    rl.close();\\n});"
                          }
                        }
                        """,
                selectedTitle.getTitle(),
                selectedTitle.getBriefDescription(),
                selectedTitle.getConcept(),
                categoryName,
                filter.difficulty().getDisplayName(),
                targetRating,
                filter.hasSubtype() ? String.format("Specific Topic: %s", filter.subtype()) : "",
                categoryName,
                selectedTitle.getConcept(),
                subtypeEmphasis,
                categoryName.toLowerCase(),
                targetRating,
                subtypeRequirement,
                categoryName, // For verification checklist: "Problem requires %s"
                categoryName, // For verification checklist: "Problem is not just 'implement %s'"
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
     */
    private Problem convertToProblem(GeneratedProblemResponse response, ProblemFilter filter) {
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
