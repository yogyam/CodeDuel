package com.coderace.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration for real-time communication using STOMP protocol
 * 
 * Message Flow:
 * 1. Clients connect to /ws endpoint using SockJS
 * 2. Clients subscribe to /topic/room/{roomId} to receive room updates
 * 3. Clients send messages to /app/* endpoints which are handled
 * by @MessageMapping
 * 4. Server broadcasts updates to all subscribers via /topic destinations
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable a simple memory-based message broker to send messages to clients
        // Clients subscribed to destinations starting with /topic will receive messages
        config.enableSimpleBroker("/topic");

        // Messages sent to /app/* will be routed to @MessageMapping methods
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register the /ws endpoint for WebSocket connections
        // SockJS fallback is enabled for browsers that don't support WebSocket
        registry.addEndpoint("/ws")
                .setAllowedOrigins(allowedOrigins.split(","))
                .withSockJS();
    }
}
