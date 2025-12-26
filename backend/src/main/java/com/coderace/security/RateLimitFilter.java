package com.coderace.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Scheduled;

/**
 * Rate Limiting Filter to prevent brute-force attacks and API abuse
 * Limits requests per IP address for authentication and expensive API endpoints
 */
@Component
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    // Store buckets per IP address
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    // Rate limits: 10 requests per minute
    private static final int MAX_REQUESTS_PER_MINUTE = 10;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Apply rate limiting to authentication and expensive API endpoints
        if (path.startsWith("/api/auth/login") ||
                path.startsWith("/api/auth/register") ||
                path.startsWith("/api/auth/codeforces-handle") ||
                path.startsWith("/api/game/generate-titles") ||
                path.startsWith("/api/game/generate-problem-from-title") ||
                path.startsWith("/api/game/create-room")) {

            String clientIP = getClientIP(request);
            Bucket bucket = resolveBucket(clientIP);

            if (bucket.tryConsume(1)) {
                // Request allowed
                log.debug("Request allowed from IP: {}", clientIP);
                filterChain.doFilter(request, response);
            } else {
                // Rate limit exceeded
                log.warn("Rate limit exceeded for IP: {} on path: {}", clientIP, path);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"error\": \"Too many requests. Please try again later.\", " +
                                "\"retryAfter\": \"60 seconds\"}");
            }
        } else {
            // Not a rate-limited endpoint, skip
            filterChain.doFilter(request, response);
        }
    }

    /**
     * Resolve or create bucket for given client IP
     */
    private Bucket resolveBucket(String clientIP) {
        return buckets.computeIfAbsent(clientIP, key -> createNewBucket());
    }

    /**
     * Create a new rate limit bucket
     * 10 requests per minute with refill of 10 tokens every 60 seconds
     */
    private Bucket createNewBucket() {
        Bandwidth limit = Bandwidth.classic(
                MAX_REQUESTS_PER_MINUTE,
                Refill.intervally(MAX_REQUESTS_PER_MINUTE, Duration.ofMinutes(1)));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Extract client IP address from request
     * Handles proxy headers (X-Forwarded-For)
     */
    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs, get the first one
            return xfHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Cleanup old buckets periodically to prevent memory leaks
     * Runs every hour to clear inactive rate limit buckets
     */
    @Scheduled(fixedRate = 3600000) // Every 1 hour (3600000ms)
    public void cleanupOldBuckets() {
        int size = buckets.size();
        buckets.clear();
        log.info("Rate limit buckets cleared: {} buckets removed", size);
    }
}
