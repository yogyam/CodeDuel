package com.coderace.config;

import com.coderace.security.WebSocketAuthInterceptor;
import com.coderace.security.WebSocketHandshakeInterceptor;
import com.coderace.security.WebSocketCsrfInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration for real-time communication using STOMP protocol
 * 
 * Message Flow:
 * 1. Clients connect to /ws endpoint using SockJS
 * 2. JWT token is validated via WebSocketAuthInterceptor
 * 3. Clients subscribe to /topic/room/{roomId} to receive room updates
 * 4. Clients send messages to /app/* endpoints which are handled
 * by @MessageMapping
 * 5. Server broadcasts updates to all subscribers via /topic destinations
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;
    private final WebSocketHandshakeInterceptor webSocketHandshakeInterceptor;
    private final WebSocketCsrfInterceptor webSocketCsrfInterceptor;

    public WebSocketConfig(
            WebSocketAuthInterceptor webSocketAuthInterceptor,
            WebSocketHandshakeInterceptor webSocketHandshakeInterceptor,
            WebSocketCsrfInterceptor webSocketCsrfInterceptor) {
        this.webSocketAuthInterceptor = webSocketAuthInterceptor;
        this.webSocketHandshakeInterceptor = webSocketHandshakeInterceptor;
        this.webSocketCsrfInterceptor = webSocketCsrfInterceptor;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable a simple memory-based message broker to send messages to clients
        // Clients subscribed to destinations starting with /topic will receive messages
        config.enableSimpleBroker("/topic");

        // Messages sent to /app/* will be routed to @MessageMapping methods
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Add JWT authentication interceptor for STOMP messages
        registration.interceptors(webSocketAuthInterceptor);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register the /ws endpoint for WebSocket connections
        // SockJS fallback is enabled for browsers that don't support WebSocket
        // Add CSRF protection and cookie extraction interceptors
        registry.addEndpoint("/ws")
                .setAllowedOrigins(allowedOrigins.split(","))
                .addInterceptors(webSocketCsrfInterceptor, webSocketHandshakeInterceptor)
                .withSockJS();
    }
}
