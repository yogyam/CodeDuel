package com.coderace.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * CSRF Protection for WebSocket connections
 * Validates Origin and Referer headers to prevent cross-site WebSocket
 * hijacking
 */
@Component
@Slf4j
public class WebSocketCsrfInterceptor implements HandshakeInterceptor {

    @Value("${frontend.url:http://localhost:5173}")
    private String allowedOrigin;

    @Value("${backend.url:http://localhost:8080}")
    private String backendUrl;

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) throws Exception {

        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpServletRequest httpRequest = servletRequest.getServletRequest();

            // Get Origin header
            String origin = httpRequest.getHeader("Origin");
            String referer = httpRequest.getHeader("Referer");

            log.debug("WebSocket handshake - Origin: {}, Referer: {}", origin, referer);

            // Validate Origin header (primary CSRF protection)
            if (!isValidOrigin(origin, referer)) {
                log.warn("WebSocket handshake rejected - Invalid origin: {} or referer: {}", origin, referer);
                return false;
            }

            log.info("WebSocket handshake accepted from origin: {}", origin);
            return true;
        }

        log.warn("WebSocket handshake rejected - Not a servlet request");
        return false;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception) {
        // No action needed after handshake
    }

    /**
     * Validate that Origin or Referer header matches allowed origins
     */
    private boolean isValidOrigin(String origin, String referer) {
        // Check Origin header first (preferred)
        if (origin != null && !origin.isEmpty()) {
            return isAllowedOrigin(origin);
        }

        // Fallback to Referer header
        if (referer != null && !referer.isEmpty()) {
            // Extract origin from referer URL
            try {
                String refererOrigin = extractOrigin(referer);
                return isAllowedOrigin(refererOrigin);
            } catch (Exception e) {
                log.warn("Failed to parse referer header: {}", referer);
                return false;
            }
        }

        // No Origin or Referer header - reject for security
        log.warn("WebSocket connection missing both Origin and Referer headers");
        return false;
    }

    /**
     * Check if origin matches allowed origins
     */
    private boolean isAllowedOrigin(String origin) {
        if (origin == null || origin.isEmpty()) {
            return false;
        }

        // Normalize origins (remove trailing slash)
        String normalizedOrigin = origin.endsWith("/")
                ? origin.substring(0, origin.length() - 1)
                : origin;
        String normalizedAllowed = allowedOrigin.endsWith("/")
                ? allowedOrigin.substring(0, allowedOrigin.length() - 1)
                : allowedOrigin;
        String normalizedBackend = backendUrl.endsWith("/")
                ? backendUrl.substring(0, backendUrl.length() - 1)
                : backendUrl;

        // Allow frontend origin or backend origin (for testing)
        return normalizedOrigin.equals(normalizedAllowed) ||
                normalizedOrigin.equals(normalizedBackend);
    }

    /**
     * Extract origin from full URL
     */
    private String extractOrigin(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }

        // Find protocol end
        int protocolEnd = url.indexOf("://");
        if (protocolEnd == -1) {
            return null;
        }

        // Find path start after protocol
        int pathStart = url.indexOf("/", protocolEnd + 3);
        if (pathStart == -1) {
            return url; // No path, entire URL is origin
        }

        return url.substring(0, pathStart);
    }
}
