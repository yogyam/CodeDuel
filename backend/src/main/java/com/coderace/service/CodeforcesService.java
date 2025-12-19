package com.coderace.service;

import com.coderace.dto.ProblemFilter;
import com.coderace.model.Problem;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
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
     * Fetches a random problem from Codeforces with specified filters
     * Uses the problemset.problems API method with tag filtering
     * Retries up to 3 times with 2-second delay on failure
     * 
     * @param filter The filter criteria (difficulty range and tags)
     * @return A random problem matching the filters, or null if none found
     */
    @Retryable(value = { RestClientException.class }, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public Problem getFilteredRandomProblem(ProblemFilter filter) {
        try {
            // Build URL with tags if provided
            String url = baseUrl + "/problemset.problems";
            if (filter != null && filter.tags() != null && !filter.tags().isEmpty()) {
                url += "?tags=" + filter.getTagsString();
                log.info("Fetching problems with tags: {}", filter.getTagsString());
            } else {
                log.info("Fetching problems without tag filter");
            }

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

            // Filter problems by difficulty range
            for (JsonNode problemNode : problemsArray) {
                // Skip problems without rating
                if (!problemNode.has("rating")) {
                    continue;
                }

                int rating = problemNode.get("rating").asInt();

                // Apply difficulty filter if specified
                if (filter != null) {
                    if (filter.minDifficulty() != null && rating < filter.minDifficulty()) {
                        continue;
                    }
                    if (filter.maxDifficulty() != null && rating > filter.maxDifficulty()) {
                        continue;
                    }
                }

                Problem problem = new Problem();
                problem.setContestId(problemNode.get("contestId").asText());
                problem.setIndex(problemNode.get("index").asText());
                problem.setName(problemNode.get("name").asText());
                problem.setType(problemNode.get("type").asText());
                problem.setRating(rating);

                // Extract tags
                List<String> tags = new ArrayList<>();
                for (JsonNode tag : problemNode.get("tags")) {
                    tags.add(tag.asText());
                }
                problem.setTags(tags);

                matchingProblems.add(problem);
            }

            // Return random problem from matching ones
            if (matchingProblems.isEmpty()) {
                log.warn("No problems found matching filter criteria");
                return null;
            }

            Problem selectedProblem = matchingProblems.get(random.nextInt(matchingProblems.size()));

            // Fetch problem description HTML
            String description = fetchProblemDescription(selectedProblem.getContestId(), selectedProblem.getIndex());
            selectedProblem.setDescription(description);

            log.info("Selected problem: {} - {} (rating: {}, tags: {})",
                    selectedProblem.getProblemId(),
                    selectedProblem.getName(),
                    selectedProblem.getRating(),
                    selectedProblem.getTags());
            return selectedProblem;

        } catch (RestClientException e) {
            log.error("REST client error fetching filtered problem: {}", e.getMessage());
            throw e; // Rethrow to trigger retry
        } catch (Exception e) {
            log.error("Unexpected error fetching filtered problem: ", e);
            return null;
        }
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
        // Use the new filtered method with a simple rating filter
        ProblemFilter filter = new ProblemFilter(rating, rating, List.of());
        return getFilteredRandomProblem(filter);
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

    /**
     * Fetches the problem description HTML from Codeforces problem page
     * Uses Jsoup to parse HTML and extract the problem statement
     * 
     * @param contestId The contest ID
     * @param index     The problem index (A, B, C, etc.)
     * @return HTML content of the problem statement, or null if error
     */
    public String fetchProblemDescription(String contestId, String index) {
        try {
            String url = "https://codeforces.com/problemset/problem/" + contestId + "/" + index;
            log.info("Fetching problem description from: {}", url);

            // Fetch and parse the HTML page with browser-like headers
            Document doc = Jsoup.connect(url)
                    .userAgent(
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.5")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("Connection", "keep-alive")
                    .timeout(10000) // 10 second timeout
                    .get();

            // Extract the problem statement div
            Element problemStatement = doc.selectFirst("div.problem-statement");

            if (problemStatement == null) {
                log.error("Could not find problem statement div for problem {}{}", contestId, index);
                return null;
            }

            // Get the HTML content of the problem statement
            String html = problemStatement.html();

            log.info("Successfully fetched description for problem {}{} ({} chars)",
                    contestId, index, html.length());
            return html;

        } catch (Exception e) {
            log.error("Error fetching problem description for {}{}: {}", contestId, index, e.getMessage());
            return null;
        }
    }

    /**
     * Gets a list of common algorithm tags for filtering
     */
    public List<String> getCommonTags() {
        return List.of(
                "dp", "greedy", "math", "implementation", "constructive algorithms",
                "data structures", "brute force", "binary search", "dfs and similar",
                "graphs", "trees", "sortings", "number theory", "combinatorics",
                "two pointers", "strings", "geometry", "bitmasks", "dsu");
    }
}
