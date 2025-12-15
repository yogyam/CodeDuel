package com.coderace.service;

import com.coderace.model.Problem;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Service to interact with Codeforces API
 * Documentation: https://codeforces.com/apiHelp
 */
@Service
@Slf4j
public class CodeforcesService {

    @Value("${codeforces.api.base-url}")
    private String baseUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final Random random;

    public CodeforcesService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.random = new Random();
    }

    /**
     * Fetches a random problem from Codeforces with the specified rating
     * Uses the problemset.problems API method
     * Retries up to 3 times with 2-second delay on failure
     * 
     * @param rating The difficulty rating (e.g., 800, 1200, 1600)
     * @return A random problem with the specified rating, or null if error
     */
    @Retryable(value = { RestClientException.class }, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public Problem fetchRandomProblem(Integer rating) {
        try {
            String url = baseUrl + "/problemset.problems";
            log.info("Fetching problems from Codeforces API with rating: {}", rating);

            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);

            // Check if API call was successful
            if (!"OK".equals(root.get("status").asText())) {
                log.error("Codeforces API error: {}", root.get("comment").asText());
                return null;
            }

            // Extract problems array
            JsonNode problemsArray = root.get("result").get("problems");
            List<Problem> matchingProblems = new ArrayList<>();

            // Filter problems by rating
            for (JsonNode problemNode : problemsArray) {
                if (problemNode.has("rating") && problemNode.get("rating").asInt() == rating) {
                    Problem problem = new Problem();
                    problem.setContestId(problemNode.get("contestId").asText());
                    problem.setIndex(problemNode.get("index").asText());
                    problem.setName(problemNode.get("name").asText());
                    problem.setType(problemNode.get("type").asText());
                    problem.setRating(problemNode.get("rating").asInt());

                    // Extract tags
                    List<String> tags = new ArrayList<>();
                    for (JsonNode tag : problemNode.get("tags")) {
                        tags.add(tag.asText());
                    }
                    problem.setTags(tags);

                    matchingProblems.add(problem);
                }
            }

            // Return random problem from matching ones
            if (matchingProblems.isEmpty()) {
                log.warn("No problems found with rating: {}", rating);
                return null;
            }

            Problem selectedProblem = matchingProblems.get(random.nextInt(matchingProblems.size()));
            log.info("Selected problem: {} - {}", selectedProblem.getProblemId(), selectedProblem.getName());
            return selectedProblem;

        } catch (RestClientException e) {
            log.error("REST client error fetching problem from Codeforces (rating: {}): {}", rating, e.getMessage());
            throw e; // Rethrow to trigger retry
        } catch (Exception e) {
            log.error("Unexpected error fetching problem from Codeforces (rating: {}): ", rating, e);
            return null;
        }
    }

    /**
     * Checks if a user has solved a specific problem after the game start time
     * Uses the user.status API method
     * Retries up to 3 times with 2-second delay on failure
     * 
     * @param handle        Codeforces handle
     * @param problemId     Problem ID (contestId + index, e.g., "1234A")
     * @param gameStartTime The time when the game started
     * @return true if user has an OK submission for this problem after game start
     */
    @Retryable(value = { RestClientException.class }, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public boolean hasUserSolvedProblem(String handle, String problemId, Instant gameStartTime) {
        try {
            String url = baseUrl + "/user.status?handle=" + handle + "&from=1&count=100";
            log.debug("Checking submissions for user: {}", handle);

            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);

            // Check if API call was successful
            if (!"OK".equals(root.get("status").asText())) {
                log.error("Codeforces API error for user {}: {}", handle, root.get("comment").asText());
                return false;
            }

            // Extract submissions array
            JsonNode submissionsArray = root.get("result");

            for (JsonNode submission : submissionsArray) {
                // Check if submission is for the current problem
                JsonNode problemNode = submission.get("problem");
                String submittedProblemId = problemNode.get("contestId").asText() +
                        problemNode.get("index").asText();

                if (!submittedProblemId.equals(problemId)) {
                    continue;
                }

                // Check if submission was made after game start
                long submissionTime = submission.get("creationTimeSeconds").asLong();
                Instant submissionInstant = Instant.ofEpochSecond(submissionTime);

                if (submissionInstant.isBefore(gameStartTime)) {
                    continue;
                }

                // Check if verdict is OK
                String verdict = submission.get("verdict").asText();
                if ("OK".equals(verdict)) {
                    log.info("User {} has solved problem {} with OK verdict", handle, problemId);
                    return true;
                }
            }

            return false;

        } catch (RestClientException e) {
            log.error("REST client error checking user {} status for problem {}: {}", handle, problemId,
                    e.getMessage());
            throw e; // Rethrow to trigger retry
        } catch (Exception e) {
            log.error("Unexpected error checking user {} status for problem {}: ", handle, problemId, e);
            return false;
        }
    }

    /**
     * Gets a list of recommended difficulty ratings
     */
    public List<Integer> getRecommendedRatings() {
        return List.of(800, 900, 1000, 1100, 1200, 1300, 1400, 1500, 1600, 1700, 1800, 1900, 2000);
    }
}
