package com.coderace.service;

import com.coderace.dto.ProblemFilter;
import com.coderace.model.Problem;
import com.coderace.model.TestCase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
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

    // Removed hasUserSolvedProblem - Using in-browser IDE instead of Codeforces
    // polling

    /**
     * Gets a list of recommended difficulty ratings
     */
    public List<Integer> getRecommendedRatings() {
        return List.of(800, 900, 1000, 1100, 1200, 1300, 1400, 1500, 1600, 1700, 1800, 1900, 2000);
    }

    /**
     * Fetches the problem description HTML and sample test cases from Codeforces
     * Uses Jsoup to scrape the problem page with enhanced headers to bypass 403
     * errors
     * 
     * @param contestId The contest ID
     * @param index     The problem index (A, B, C, etc.)
     * @return HTML content of the problem statement, or null if error
     */
    private String fetchProblemDescription(String contestId, String index) {
        int maxRetries = 3;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                String url = "https://codeforces.com/problemset/problem/" + contestId + "/" + index;
                log.info("Fetching problem description from: {} (attempt {}/{})", url, attempt, maxRetries);

                // Add delay between retries to avoid rate limiting
                if (attempt > 1) {
                    Thread.sleep(2000 * attempt); // 2s, 4s, 6s
                }

                // Fetch and parse the HTML page with enhanced browser-like headers
                Document doc = Jsoup.connect(url)
                        .userAgent(
                                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .header("Accept",
                                "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
                        .header("Accept-Language", "en-US,en;q=0.9")
                        .header("Accept-Encoding", "gzip, deflate, br")
                        .header("Connection", "keep-alive")
                        .header("Upgrade-Insecure-Requests", "1")
                        .header("Sec-Fetch-Dest", "document")
                        .header("Sec-Fetch-Mode", "navigate")
                        .header("Sec-Fetch-Site", "none")
                        .header("Cache-Control", "max-age=0")
                        .referrer("https://codeforces.com/problemset")
                        .timeout(15000) // 15 second timeout
                        .followRedirects(true)
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

            } catch (org.jsoup.HttpStatusException e) {
                if (e.getStatusCode() == 403) {
                    log.warn("HTTP 403 for {}{} on attempt {}/{}", contestId, index, attempt, maxRetries);
                    if (attempt == maxRetries) {
                        log.error("Failed to fetch after {} attempts - Codeforces blocking requests", maxRetries);
                        return null;
                    }
                } else {
                    log.error("HTTP error fetching {}{}: Status={}", contestId, index, e.getStatusCode());
                    return null;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Interrupted while waiting between retries");
                return null;
            } catch (Exception e) {
                log.error("Error fetching problem description for {}{}: {}", contestId, index, e.getMessage());
                if (attempt == maxRetries) {
                    return null;
                }
            }
        }

        return null;
    }

    /**
     * Scrapes sample test cases from Codeforces problem page
     * Returns list of TestCase objects with input/output pairs
     * 
     * @param contestId The contest ID
     * @param index     The problem index
     * @return List of sample test cases
     */
    public List<TestCase> scrapeSampleTestCases(String contestId, String index) {
        List<TestCase> testCases = new ArrayList<>();

        try {
            String url = "https://codeforces.com/problemset/problem/" + contestId + "/" + index;
            log.info("Scraping sample test cases from: {}", url);

            Document doc = Jsoup.connect(url)
                    .userAgent(
                            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept",
                            "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("Connection", "keep-alive")
                    .header("Upgrade-Insecure-Requests", "1")
                    .header("Sec-Fetch-Dest", "document")
                    .header("Sec-Fetch-Mode", "navigate")
                    .header("Sec-Fetch-Site", "none")
                    .referrer("https://codeforces.com/problemset")
                    .timeout(15000)
                    .followRedirects(true)
                    .get();

            // Find sample test sections
            Elements inputDivs = doc.select("div.input pre");
            Elements outputDivs = doc.select("div.output pre");

            int count = Math.min(inputDivs.size(), outputDivs.size());
            String problemId = contestId + index;

            for (int i = 0; i < count; i++) {
                String input = inputDivs.get(i).text();
                String output = outputDivs.get(i).text();

                TestCase testCase = new TestCase();
                testCase.setProblemId(problemId);
                testCase.setInput(input);
                testCase.setExpectedOutput(output);
                testCase.setType("SAMPLE");
                testCase.setIsHidden(false);
                testCase.setCreatedAt(LocalDateTime.now());

                testCases.add(testCase);
                log.info("Scraped sample test case {} for problem {}{}", i + 1, contestId, index);
            }

            log.info("Successfully scraped {} sample test cases for problem {}{}", count, contestId, index);

        } catch (Exception e) {
            log.error("Error scraping test cases for {}{}: {}", contestId, index, e.getMessage());
        }

        return testCases;
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
