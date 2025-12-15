package com.coderace.config;

import com.coderace.service.GameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * Handles WebSocket lifecycle events
 * Cleans up users when they disconnect
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {
    
    private final GameService gameService;
    
    /**
     * Called when a user disconnects from WebSocket
     * Removes the user from their room
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        
        String sessionId = headerAccessor.getSessionId();
        String roomId = (String) headerAccessor.getSessionAttributes().get("roomId");
        
        if (roomId != null) {
            log.info("User disconnected from room {}: session {}", roomId, sessionId);
            gameService.removeUserFromRoom(roomId, sessionId);
        }
    }
}
