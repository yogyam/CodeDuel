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
    private final CodeExecutionService codeExecutionService;
    private final SubmissionService submissionService;
    private final com.coderace.repository.UserRepository userRepository;
    private final com.coderace.repository.TestCaseRepository testCaseRepository;
    private final ProblemGenerationService problemGenerationService;

    @Value("${codeforces.polling.interval:5000}")
    private long pollingInterval;

    public GameService(CodeforcesService codeforcesService,
            SimpMessagingTemplate messagingTemplate,
            CodeExecutionService codeExecutionService,
            SubmissionService submissionService,
            com.coderace.repository.UserRepository userRepository,
            com.coderace.repository.TestCaseRepository testCaseRepository,
            ProblemGenerationService problemGenerationService) {
        this.codeforcesService = codeforcesService;
        this.messagingTemplate = messagingTemplate;
        this.codeExecutionService = codeExecutionService;
        this.submissionService = submissionService;
        this.userRepository = userRepository;
        this.testCaseRepository = testCaseRepository;
        this.problemGenerationService = problemGenerationService;
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
     * @param filter    Problem filter with difficulty range and tags
     * @return true if game started successfully
     */
    public boolean startGame(String roomId, String sessionId, com.coderace.dto.ProblemFilter filter) {
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

        // Generate problem using Gemini AI
        Problem problem;
        try {
            problem = problemGenerationService.generateProblem(filter);
        } catch (Exception e) {
            log.error("Failed to generate problem for room {} with filters: {}", roomId, filter, e);
            return false;
        }

        // Update room state
        room.setCurrentProblem(problem);
        room.setSelectedRating(filter.minDifficulty()); // Store min difficulty for reference
        room.setState(GameRoom.GameState.STARTED);
        room.setGameStartTime(Instant.now());

        // Update all users' status to SOLVING
        room.getUserList().forEach(u -> u.setStatus(User.UserStatus.SOLVING));

        log.info("Game started in room {} with generated problem {} (filters: {})",
                roomId, problem.getProblemId(), filter);
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
     * DEPRECATED: Disabled in favor of in-browser IDE system
     * Old polling system that checked Codeforces API for solutions
     * 
     * Periodically checks for game winners by polling Codeforces API
     * Runs every 5 seconds (configurable via application.properties)
     * 
     * For each active game:
     * 1. Get all users in the room
     * 2. Check each user's recent submissions via Codeforces API
     * 3. If a user has solved the problem after game start time, declare them
     * winner
     * 4. Broadcast winner announcement to all users in the room
     */
    // @Scheduled(fixedDelayString = "${codeforces.polling.interval:5000}")
    // public void checkForWinners() {
    // // Disabled - using in-browser IDE with Piston API instead
    // }

    // /**
    // * Checks a specific room for winners
    // * Synchronized on the room object to prevent race conditions
    // *
    // * @param room The room to check
    // */
    // private void checkRoomForWinner(GameRoom room) {
    // // Disabled - using in-browser IDE with Piston API instead
    // }

    /**
     * Handles code submission from a user in a game room
     * Executes code against test cases and updates game state
     * 
     * @param roomId    Room ID
     * @param sessionId User's session ID
     * @param username  Authenticated username
     * @param code      Submitted code
     * @param language  Programming language
     * @return Submission verdict
     */
    public com.coderace.dto.SubmissionVerdict handleCodeSubmission(
            String roomId, String sessionId, String username, String code, String language) {

        GameRoom room = activeRooms.get(roomId);
        if (room == null) {
            throw new IllegalArgumentException("Room not found: " + roomId);
        }

        if (room.getState() != GameRoom.GameState.STARTED) {
            throw new IllegalStateException("Game not started");
        }

        // Find the user
        User user = room.getUserList().stream()
                .filter(u -> u.getSessionId().equals(sessionId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("User not in room"));

        String problemId = room.getCurrentProblem().getProblemId();

        // Get test cases for this problem
        java.util.List<com.coderace.model.TestCase> testCases = testCaseRepository.findByProblemId(problemId);

        // For MVP: If no test cases, create a dummy one (temporary until LLM generation
        // works)
        if (testCases.isEmpty()) {
            log.warn("No test cases found for problem {}. Using dummy test case.", problemId);
            com.coderace.model.TestCase dummy = new com.coderace.model.TestCase();
            dummy.setProblemId(problemId);
            dummy.setInput("1");
            dummy.setExpectedOutput("1");
            dummy.setType("DUMMY");
            dummy.setIsHidden(false);
            testCases = java.util.List.of(dummy);
        }

        log.info("Executing code for user {} in room {}. Testing against {} test cases",
                user.getCodeforcesHandle(), roomId, testCases.size());

        // Execute code against test cases
        com.coderace.dto.SubmissionVerdict verdict = codeExecutionService.verifySubmission(code, language, testCases);

        // Find the authenticated user entity for saving submission
        com.coderace.entity.User userEntity = userRepository
                .findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found in database: " + username));

        // Save submission
        submissionService.saveSubmission(userEntity, roomId, problemId, code, language, verdict);

        // Check if user won
        if (verdict.isAccepted()) {
            synchronized (room) {
                if (room.getState() == GameRoom.GameState.STARTED) {
                    user.setStatus(User.UserStatus.WON);
                    room.setWinnerId(sessionId);
                    room.setState(GameRoom.GameState.FINISHED);

                    log.info("User {} won in room {}!", user.getCodeforcesHandle(), roomId);
                }
            }
        }

        return verdict;
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
