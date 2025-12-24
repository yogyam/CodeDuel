package com.coderace.controller;

import com.coderace.dto.CreateRoomRequest;
import com.coderace.model.GameRoom;
import com.coderace.service.GameService;
import com.coderace.service.ProblemGenerationService;
import com.coderace.constants.GameConstants;
import jakarta.validation.Valid;
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
@Slf4j
public class GameController {

    private final GameService gameService;
    private final ProblemGenerationService problemGenerationService;

    public GameController(GameService gameService, ProblemGenerationService problemGenerationService) {
        this.gameService = gameService;
        this.problemGenerationService = problemGenerationService;
    }

    /**
     * Creates a new game room
     * The creator becomes the host of the room
     * 
     * @param request Contains the Codeforces handle of the host
     * @return Room ID of the created room
     */
    @PostMapping("/create-room")
    public ResponseEntity<Map<String, String>> createRoom(@RequestBody CreateRoomRequest request) {

        // Get authenticated user from SecurityContext
        org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof com.coderace.entity.User)) {
            log.error("User not authenticated or invalid principal type");
            return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
        }

        com.coderace.entity.User user = (com.coderace.entity.User) authentication.getPrincipal();
        String username = user.getUsername();

        log.info("Creating room for user: {} (username: {})", request.getCodeforcesHandle(), username);

        String roomId = gameService.createRoom(request.getCodeforcesHandle(), username);

        Map<String, String> response = new HashMap<>();
        response.put("roomId", roomId);
        response.put("message", "Room created successfully");

        log.info("Room created with ID: {} for username: {}", roomId, username);
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

    /**
     * Step 1: Generate 3 problem title options
     * Part of two-step problem generation flow
     * 
     * @param request Filter with category, difficulty, and optional subtype
     * @return 3 problem title options for user to choose from
     */
    @PostMapping("/generate-titles")
    public ResponseEntity<com.coderace.dto.GenerateTitlesResponse> generateTitles(
            @Valid @RequestBody com.coderace.dto.ProblemFilter filter) {

        log.info("Generating title options for category: {}, difficulty: {}",
                filter.category(), filter.difficulty());

        com.coderace.dto.GenerateTitlesResponse titles = problemGenerationService.generateProblemTitles(
                filter, GameConstants.ROOM_ID_REST_API);

        return ResponseEntity.ok(titles);
    }

    /**
     * Step 2: Generate full problem from selected title
     * 
     * @param request Contains filter and selected title option
     * @return Complete problem with test cases
     */
    @PostMapping("/generate-problem-from-title")
    public ResponseEntity<com.coderace.model.Problem> generateProblemFromTitle(
            @Valid @RequestBody com.coderace.dto.GenerateFullProblemRequest request) {

        log.info("Generating full problem from selected title: {}",
                request.getSelectedTitle().getTitle());

        try {
            com.coderace.model.Problem problem = problemGenerationService.generateProblemFromTitle(
                    request.getFilter(),
                    request.getSelectedTitle(),
                    GameConstants.ROOM_ID_REST_API);

            // Validate problem was generated successfully
            if (problem == null) {
                log.error("Problem generation returned null for title: {}", request.getSelectedTitle().getTitle());
                return ResponseEntity.internalServerError().build();
            }

            // Cache the problem so it can be retrieved when game starts via WebSocket
            gameService.cacheGeneratedProblem(problem);

            return ResponseEntity.ok(problem);

        } catch (Exception e) {
            log.error("Error generating problem from title: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
