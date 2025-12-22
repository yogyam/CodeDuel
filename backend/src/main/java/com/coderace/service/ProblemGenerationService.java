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

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    @Value("${gemini.temperature:0.7}")
    private double temperature;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final TestCaseRepository testCaseRepository;

    public ProblemGenerationService(RestTemplate restTemplate, ObjectMapper objectMapper,
            TestCaseRepository testCaseRepository) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.testCaseRepository = testCaseRepository;
    }

    /**
     * Generate a new problem using Gemini API
     *
     * @param filter Problem difficulty and tags
     * @return Generated problem with test cases
     */
    @Transactional
    public Problem generateProblem(ProblemFilter filter) {
        log.info("Generating problem with Gemini: difficulty={}, tags={}",
                filter.minDifficulty(), filter.tags());

        try {
            // Build prompt for Gemini
            String prompt = buildPrompt(filter);

            // Call Gemini API
            GeneratedProblemResponse geminiResponse = callGeminiAPI(prompt);

            // Convert to Problem entity
            Problem problem = convertToProblem(geminiResponse, filter);

            // Save test cases
            saveTestCases(problem, geminiResponse);

            log.info("Successfully generated problem: {}", problem.getName());
            return problem;

        } catch (Exception e) {
            log.error("Failed to generate problem: {}", e.getMessage(), e);
            throw new RuntimeException("Problem generation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Build the prompt for Gemini API
     */
    private String buildPrompt(ProblemFilter filter) {
        int difficulty = (filter.minDifficulty() + filter.maxDifficulty()) / 2;
        String difficultyLevel = getDifficultyLevel(difficulty);
        String tags = filter.tags().isEmpty() ? "general algorithms" : String.join(", ", filter.tags());

        return String.format("""
                Generate a competitive programming problem with the following specifications:

                Difficulty: %d (%s level)
                Topics: %s

                Requirements:
                1. **Problem Statement**: Write a clear, engaging problem (200-400 words) with a real-world scenario
                2. **Input Format**: Precise specification of input format
                3. **Output Format**: Precise specification of output format
                4. **Constraints**: List all constraints (input size, time limits, etc.)
                5. **Sample Test Cases**: Provide 2-3 examples with step-by-step explanations
                6. **Hidden Test Cases**: Generate 10 test cases covering:
                   - Basic cases (2 tests)
                   - Edge cases (3 tests: empty, minimum, maximum)
                   - Corner cases (3 tests)
                   - Random cases (2 tests)

                Format your response as valid JSON:
                {
                  "title": "Problem Title (creative and engaging)",
                  "description": "Full problem statement with HTML formatting (<p>, <strong>, <code>, etc.)",
                  "inputFormat": "Input specification",
                  "outputFormat": "Output specification",
                  "constraints": ["constraint1", "constraint2", ...],
                  "sampleTests": [
                    {"input": "test input", "output": "expected output", "explanation": "why this output"}
                  ],
                  "hiddenTests": [
                    {"input": "test input", "output": "expected output"}
                  ],
                  "difficulty": %d,
                  "tags": ["%s"]
                }

                Important:
                - Make the problem interesting and fun to solve
                - Ensure test cases cover all edge cases
                - Verify all outputs are correct
                - Use proper HTML formatting in description
                """, difficulty, difficultyLevel, tags, difficulty, tags);
    }

    /**
     * Call Gemini API with the prompt
     */
    private GeneratedProblemResponse callGeminiAPI(String prompt) {
        try {
            // Build request body
            Map<String, Object> requestBody = new HashMap<>();

            List<Map<String, Object>> contents = new ArrayList<>();
            Map<String, Object> content = new HashMap<>();

            List<Map<String, String>> parts = new ArrayList<>();
            parts.add(Map.of("text", prompt));

            content.put("parts", parts);
            contents.add(content);

            requestBody.put("contents", contents);

            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("temperature", temperature);
            generationConfig.put("maxOutputTokens", 4000);
            requestBody.put("generationConfig", generationConfig);

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // Add API key to URL
            String urlWithKey = apiUrl + "?key=" + apiKey;

            // Call API
            log.debug("Calling Gemini API...");
            String response = restTemplate.postForObject(urlWithKey, request, String.class);

            // Parse response
            JsonNode root = objectMapper.readTree(response);
            JsonNode candidatesNode = root.get("candidates");

            if (candidatesNode == null || candidatesNode.isEmpty()) {
                throw new RuntimeException("No response from Gemini API");
            }

            String jsonContent = candidatesNode.get(0)
                    .get("content")
                    .get("parts")
                    .get(0)
                    .get("text")
                    .asText();

            // Extract JSON from markdown code blocks if present
            jsonContent = extractJsonFromMarkdown(jsonContent);

            // Parse the generated problem
            GeneratedProblemResponse problemResponse = objectMapper.readValue(jsonContent,
                    GeneratedProblemResponse.class);

            log.info("Successfully parsed Gemini response");
            return problemResponse;

        } catch (Exception e) {
            log.error("Error calling Gemini API: {}", e.getMessage(), e);
            throw new RuntimeException("Gemini API call failed", e);
        }
    }

    /**
     * Extract JSON from markdown code blocks
     */
    private String extractJsonFromMarkdown(String content) {
        // Remove markdown code blocks if present
        if (content.contains("```json")) {
            int start = content.indexOf("```json") + 7;
            int end = content.lastIndexOf("```");
            if (start > 7 && end > start) {
                return content.substring(start, end).trim();
            }
        } else if (content.contains("```")) {
            int start = content.indexOf("```") + 3;
            int end = content.lastIndexOf("```");
            if (start > 3 && end > start) {
                return content.substring(start, end).trim();
            }
        }
        return content.trim();
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
        problem.setLlmModel("gemini-pro");
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
