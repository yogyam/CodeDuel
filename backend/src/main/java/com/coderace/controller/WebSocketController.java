package com.coderace.controller;

import com.coderace.dto.GameStateUpdate;
import com.coderace.dto.JoinRoomRequest;
import com.coderace.dto.StartGameRequest;
import com.coderace.model.GameRoom;
import com.coderace.model.User;
import com.coderace.service.GameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

/**
 * WebSocket Controller for handling real-time game events
 * 
 * Message Flow:
 * 1. Client sends JOIN message -> Server adds user to room -> Broadcast room
 * state to all
 * 2. Host sends START message -> Server fetches problem -> Broadcast game
 * started to all
 * 3. Server polls Codeforces -> Detects winner -> Broadcast winner to all
 * 4. Client disconnects -> Server removes user -> Broadcast updated room state
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {

    private final GameService gameService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Handles user joining a room
     * Path: /app/game/{roomId}/join
     * Broadcast destination: /topic/room/{roomId}
     * 
     * @param roomId         The ID of the room to join
     * @param request        Contains the Codeforces handle
     * @param headerAccessor Used to get the WebSocket session ID
     */
    @MessageMapping("/game/{roomId}/join")
    public void joinRoom(@DestinationVariable String roomId,
            @Payload JoinRoomRequest request,
            SimpMessageHeaderAccessor headerAccessor) {

        String sessionId = headerAccessor.getSessionId();
        log.info("User {} joining room {} with session {}", request.getCodeforcesHandle(), roomId, sessionId);

        // Add user to room
        User user = gameService.addUserToRoom(roomId, request.getCodeforcesHandle(), sessionId);

        if (user == null) {
            log.error("Failed to add user to room {}", roomId);
            return;
        }

        // Store room ID in session attributes for cleanup on disconnect
        headerAccessor.getSessionAttributes().put("roomId", roomId);

        // Broadcast updated room state to all users in the room
        GameRoom room = gameService.getRoom(roomId);
        GameStateUpdate update = new GameStateUpdate(
                room,
                user.getCodeforcesHandle() + " joined the room");

        messagingTemplate.convertAndSend("/topic/room/" + roomId, update);
        log.info("Broadcasted join event to room {}", roomId);
    }

    /**
     * Handles starting the game
     * Only the host can start the game
     * Path: /app/game/{roomId}/start
     * Broadcast destination: /topic/room/{roomId}
     * 
     * @param roomId         The ID of the room
     * @param request        Contains the selected difficulty rating
     * @param headerAccessor Used to verify the user is the host
     */
    @MessageMapping("/game/{roomId}/start")
    public void startGame(@DestinationVariable String roomId,
            @Payload StartGameRequest request,
            SimpMessageHeaderAccessor headerAccessor) {

        String sessionId = headerAccessor.getSessionId();
        log.info("Start game request for room {} with rating {} by session {}",
                roomId, request.getRating(), sessionId);

        // Start the game (fetches problem and updates state)
        boolean started = gameService.startGame(roomId, sessionId, request.getRating());

        if (!started) {
            log.error("Failed to start game for room {}", roomId);
            return;
        }

        // Broadcast game started to all users
        GameRoom room = gameService.getRoom(roomId);
        GameStateUpdate update = new GameStateUpdate(
                room,
                "Game started! Solve the problem.");

        messagingTemplate.convertAndSend("/topic/room/" + roomId, update);
        log.info("Game started and broadcasted to room {}", roomId);
    }
}
