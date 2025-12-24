package com.coderace.service;

import com.coderace.dto.GeneratedProblemResponse;
import com.coderace.dto.ProblemFilter;
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
     * Generate a new problem using Gemini API
     *
     * @param filter Problem difficulty and tags
     * @return Generated problem with test cases
     */
    @Transactional
    public Problem generateProblem(ProblemFilter filter, String roomId) {
        log.info("Generating problem with description: {}", filter.description());

        try {
            // Step 1: Building prompt
            broadcastStatus(roomId, "Building problem prompt...");
            String prompt = buildPrompt(filter);

            // Step 2: Calling Gemini API
            broadcastStatus(roomId, "Generating problem with AI (this may take 10-30s)...");
            GeneratedProblemResponse response = callOpenAI(prompt);

            // Step 3: Converting to problem
            broadcastStatus(roomId, "Processing problem details...");
            Problem problem = convertToProblem(response, filter);

            // Step 4: Saving test cases
            broadcastStatus(roomId, "Creating test cases...");
            saveTestCases(problem, response);

            broadcastStatus(roomId, "Problem ready!");
            log.info("Successfully generated problem: {}", problem.getName());
            return problem;

        } catch (Exception e) {
            broadcastStatus(roomId, "Failed to generate problem");
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
     * Build the prompt for OpenAI API
     */
    private String buildPrompt(ProblemFilter filter) {
        String userDescription = filter.hasDescription()
                ? filter.description()
                : "a general competitive programming problem";

        return String.format(
                """
                        You are an expert competitive programming problem setter. Generate a HIGH-QUALITY competitive programming problem based on:
                        "%s"

                        CRITICAL REQUIREMENTS:
                        1. The problem MUST be SOLVABLE and ALGORITHMICALLY SOUND
                        2. DO NOT ask students to simply "implement X algorithm" - instead, create a scenario that REQUIRES that algorithm
                        3. Test cases MUST NOT contradict the stated constraints
                        4. Include explicit time/space complexity requirements when relevant
                        5. All test outputs must be VERIFIED CORRECT

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

                        VERIFICATION CHECKLIST (verify before outputting):
                        ✓ All test inputs satisfy the stated constraints
                        ✓ All test outputs are algorithmically correct
                        ✓ Problem requires the specified concept/algorithm
                        ✓ Problem is not just "implement X"
                        ✓ Time/space complexity stated if relevant
                        ✓ Sample explanations walk through the solution

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
                          "difficulty": 1500,
                          "tags": ["algorithm", "data-structure", "technique"]
                        }
                        """,
                userDescription);
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

        html.append("<div class='problem-input-output'>");
        html.append("<h3>Input Format</h3>");
        html.append("<p>").append(response.getInputFormat()).append("</p>");

        html.append("<h3>Output Format</h3>");
        html.append("<p>").append(response.getOutputFormat()).append("</p>");
        html.append("</div>");

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
