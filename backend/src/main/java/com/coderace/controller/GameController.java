package com.coderace.controller;

import com.coderace.dto.CreateRoomRequest;
import com.coderace.model.GameRoom;
import com.coderace.service.GameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for game room management
 * Handles HTTP requests for creating rooms and getting room information
 */
@RestController
@RequestMapping("/api/game")
@RequiredArgsConstructor
@Slf4j
public class GameController {
    
    private final GameService gameService;
    
    /**
     * Creates a new game room
     * The creator becomes the host of the room
     * 
     * @param request Contains the Codeforces handle of the host
     * @return Room ID of the created room
     */
    @PostMapping("/create-room")
    public ResponseEntity<Map<String, String>> createRoom(@RequestBody CreateRoomRequest request) {
        log.info("Creating room for user: {}", request.getCodeforcesHandle());
        
        String roomId = gameService.createRoom(request.getCodeforcesHandle());
        
        Map<String, String> response = new HashMap<>();
        response.put("roomId", roomId);
        response.put("message", "Room created successfully");
        
        log.info("Room created with ID: {}", roomId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Gets information about a specific room
     * 
     * @param roomId The ID of the room
     * @return Room information or error if not found
     */
    @GetMapping("/room/{roomId}")
    public ResponseEntity<?> getRoomInfo(@PathVariable String roomId) {
        GameRoom room = gameService.getRoom(roomId);
        
        if (room == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Room not found");
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("roomId", room.getRoomId());
        response.put("state", room.getState());
        response.put("users", room.getUserList());
        response.put("problem", room.getCurrentProblem());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "CodeRace Backend");
        return ResponseEntity.ok(response);
    }
}
