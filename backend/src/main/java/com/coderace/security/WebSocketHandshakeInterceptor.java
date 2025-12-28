package com.coderace.security;

import jakarta.servlet.http.Cookie;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * HTTP Handshake Interceptor for WebSocket connections
 * Extracts cookies from HTTP request and stores them in WebSocket session
 * attributes
 * This allows WebSocketAuthInterceptor to access cookies during STOMP CONNECT
 */
@Component
@Slf4j
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(
            @NonNull ServerHttpRequest request,
            @NonNull ServerHttpResponse response,
            @NonNull WebSocketHandler wsHandler,
            @NonNull Map<String, Object> attributes) throws Exception {

        log.debug("WebSocket HTTP handshake - extracting cookies");

        // Extract cookies from HTTP request and store in session attributes
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            Cookie[] cookies = servletRequest.getServletRequest().getCookies();

            if (cookies != null && cookies.length > 0) {
                // Store cookies in session attributes for WebSocketAuthInterceptor to access
                attributes.put("cookies", cookies);
                log.debug("Stored {} cookies in WebSocket session attributes", cookies.length);
            } else {
                log.debug("No cookies found in WebSocket handshake request");
            }
        }

        return true; // Allow handshake to proceed (auth will happen in WebSocketAuthInterceptor)
    }

    @Override
    public void afterHandshake(
            @NonNull ServerHttpRequest request,
            @NonNull ServerHttpResponse response,
            @NonNull WebSocketHandler wsHandler,
            @Nullable Exception exception) {
        // No action needed after handshake
        if (exception != null) {
            log.error("WebSocket handshake completed with exception: {}", exception.getMessage());
        }
    }
}
