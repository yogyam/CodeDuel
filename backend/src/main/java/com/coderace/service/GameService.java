package com.coderace.service;

import com.coderace.dto.GameStateUpdate;
import com.coderace.model.GameRoom;
import com.coderace.model.Problem;
import com.coderace.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing game rooms and game logic
 * Uses in-memory ConcurrentHashMap for storing active rooms (no database for
 * MVP)
 */
@Service
@Slf4j
public class GameService {

    // In-memory storage for active game rooms
    private final ConcurrentHashMap<String, GameRoom> activeRooms = new ConcurrentHashMap<>();

    private final CodeforcesService codeforcesService;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${codeforces.polling.interval:5000}")
    private long pollingInterval;

    public GameService(CodeforcesService codeforcesService, SimpMessagingTemplate messagingTemplate) {
        this.codeforcesService = codeforcesService;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Creates a new game room with a unique ID
     * 
     * @param hostHandle Codeforces handle of the host
     * @return The unique room ID
     */
    public String createRoom(String hostHandle) {
        String roomId = generateRoomId();
        GameRoom room = new GameRoom(roomId);

        activeRooms.put(roomId, room);
        log.info("Created room {} for host {}", roomId, hostHandle);

        return roomId;
    }

    /**
     * Adds a user to an existing room
     * 
     * @param roomId    The room to join
     * @param handle    Codeforces handle
     * @param sessionId WebSocket session ID
     * @return The created User object, or null if room doesn't exist
     */
    public User addUserToRoom(String roomId, String handle, String sessionId) {
        GameRoom room = activeRooms.get(roomId);

        if (room == null) {
            log.error("Room {} not found", roomId);
            return null;
        }

        // First user to join becomes the host
        boolean isHost = room.getUserList().isEmpty();
        User user = new User(handle, sessionId, isHost);

        room.addUser(user);
        log.info("Added user {} to room {} (host: {})", handle, roomId, isHost);

        return user;
    }

    /**
     * Starts the game by fetching a problem and updating room state
     * Only the host can start the game
     * 
     * @param roomId    The room ID
     * @param sessionId Session ID of the user requesting to start
     * @param rating    Difficulty rating for the problem
     * @return true if game started successfully
     */
    public boolean startGame(String roomId, String sessionId, Integer rating) {
        GameRoom room = activeRooms.get(roomId);

        if (room == null) {
            log.error("Room {} not found", roomId);
            return false;
        }

        // Verify the user is the host
        User user = room.getUsers().get(sessionId);
        if (user == null || !user.isHost()) {
            log.error("User {} is not the host of room {}", sessionId, roomId);
            return false;
        }

        // Verify room is in WAITING state
        if (room.getState() != GameRoom.GameState.WAITING) {
            log.error("Room {} is not in WAITING state", roomId);
            return false;
        }

        // Fetch a random problem
        Problem problem = codeforcesService.fetchRandomProblem(rating);
        if (problem == null) {
            log.error("Failed to fetch problem for room {}", roomId);
            return false;
        }

        // Update room state
        room.setCurrentProblem(problem);
        room.setSelectedRating(rating);
        room.setState(GameRoom.GameState.STARTED);
        room.setGameStartTime(Instant.now());

        // Update all users' status to SOLVING
        room.getUserList().forEach(u -> u.setStatus(User.UserStatus.SOLVING));

        log.info("Game started in room {} with problem {}", roomId, problem.getProblemId());
        return true;
    }

    /**
     * Gets a room by ID
     * 
     * @param roomId The room ID
     * @return The GameRoom or null if not found
     */
    public GameRoom getRoom(String roomId) {
        return activeRooms.get(roomId);
    }

    /**
     * Removes a user from a room (called on disconnect)
     * 
     * @param roomId    The room ID
     * @param sessionId The session ID of the user
     */
    public void removeUserFromRoom(String roomId, String sessionId) {
        GameRoom room = activeRooms.get(roomId);

        if (room == null) {
            return;
        }

        room.removeUser(sessionId);
        log.info("Removed user {} from room {}", sessionId, roomId);

        // Clean up empty rooms
        if (room.isEmpty()) {
            activeRooms.remove(roomId);
            log.info("Removed empty room {}", roomId);
        }
    }

    /**
     * Scheduled task that polls Codeforces API to check for winners
     * Runs every 5 seconds (configurable via application.properties)
     * 
     * For each active game:
     * 1. Get all users in the room
     * 2. Check each user's recent submissions via Codeforces API
     * 3. If a user has solved the problem after game start time, declare them
     * winner
     * 4. Broadcast winner announcement to all users in the room
     */
    @Scheduled(fixedDelayString = "${codeforces.polling.interval:5000}")
    public void checkForWinners() {
        // Iterate through all active rooms
        activeRooms.values().stream()
                .filter(room -> room.getState() == GameRoom.GameState.STARTED)
                .forEach(this::checkRoomForWinner);
    }

    /**
     * Checks a specific room for winners
     * Synchronized on the room object to prevent race conditions
     * 
     * @param room The room to check
     */
    private void checkRoomForWinner(GameRoom room) {
        // Skip if game already finished
        if (room.getState() == GameRoom.GameState.FINISHED) {
            return;
        }

        String problemId = room.getCurrentProblem().getProblemId();
        Instant gameStartTime = room.getGameStartTime();

        // Check each user's submissions
        for (User user : room.getUserList()) {
            if (user.getStatus() == User.UserStatus.WON) {
                continue; // Skip users who already won
            }

            boolean solved = codeforcesService.hasUserSolvedProblem(
                    user.getCodeforcesHandle(),
                    problemId,
                    gameStartTime);

            if (solved) {
                // Synchronize on room to prevent multiple winners
                synchronized (room) {
                    // Double-check game state after acquiring lock
                    if (room.getState() == GameRoom.GameState.FINISHED) {
                        return;
                    }

                    // Declare winner
                    user.setStatus(User.UserStatus.WON);
                    room.setWinnerId(user.getSessionId());
                    room.setState(GameRoom.GameState.FINISHED);

                    log.info("User {} won the game in room {}!", user.getCodeforcesHandle(), room.getRoomId());

                    // Broadcast winner announcement directly
                    GameStateUpdate update = new GameStateUpdate(
                            room,
                            user.getCodeforcesHandle() + " won the race!");

                    messagingTemplate.convertAndSend("/topic/room/" + room.getRoomId(), update);
                    return; // Exit after declaring winner
                }
            }
        }
    }

    /**
     * Generates a unique 6-character room ID
     */
    private String generateRoomId() {
        return UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    /**
     * Gets the count of active rooms (for monitoring)
     */
    public int getActiveRoomCount() {
        return activeRooms.size();
    }
}
