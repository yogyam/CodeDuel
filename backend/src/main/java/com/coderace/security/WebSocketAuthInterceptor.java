package com.coderace.security;

import com.coderace.security.JwtUtil;
import jakarta.servlet.http.Cookie;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * WebSocket Channel Interceptor for JWT Authentication
 * Validates JWT tokens from cookies or Authorization header on WebSocket
 * CONNECT messages
 */
@Component
@Slf4j
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;

    public WebSocketAuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = null;

            // PRIORITY 1: Check cookies for JWT (httpOnly cookies from new auth flow)
            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
            if (sessionAttributes != null) {
                Cookie[] cookies = (Cookie[]) sessionAttributes.get("cookies");
                if (cookies != null) {
                    for (Cookie cookie : cookies) {
                        if (jwtUtil.getCookieName().equals(cookie.getName())) {
                            token = cookie.getValue();
                            log.debug("Found JWT in cookie for WebSocket connection");
                            break;
                        }
                    }
                }
            }

            // PRIORITY 2: Fallback to Authorization header (backward compatibility)
            if (token == null) {
                String authHeader = accessor.getFirstNativeHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    token = authHeader.substring(7);
                    log.debug("Found JWT in Authorization header for WebSocket connection");
                }
            }

            if (token == null) {
                log.warn("WebSocket connection rejected: No JWT token found in cookies or Authorization header");
                throw new IllegalArgumentException("Authentication required for WebSocket connection");
            }

            try {
                // Validate token
                if (!jwtUtil.validateToken(token)) {
                    log.warn("WebSocket connection rejected: Invalid JWT token");
                    throw new IllegalArgumentException("Invalid JWT token");
                }

                // Extract user information from JWT
                String username = jwtUtil.getUsernameFromToken(token);
                Long userId = jwtUtil.getUserIdFromToken(token);
                String email = jwtUtil.getEmailFromToken(token);

                // Store user info in WebSocket session attributes for later use
                Map<String, Object> currentSessionAttributes = accessor.getSessionAttributes();
                if (currentSessionAttributes != null) {
                    currentSessionAttributes.put("username", username);
                    currentSessionAttributes.put("userId", userId);
                    currentSessionAttributes.put("email", email);
                }

                log.info("WebSocket authenticated for user: {} ({})", username, email);

                // Create authentication object
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        username, null, null);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                accessor.setUser(authentication);

                return message;

            } catch (Exception e) {
                log.error("WebSocket authentication failed: {}", e.getMessage());
                throw new IllegalArgumentException("Authentication failed: " + e.getMessage());
            }
        }

        return message;
    }
}
