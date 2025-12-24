package com.coderace.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a game room where users compete
 */
@Data
@NoArgsConstructor
public class GameRoom {
    private String roomId;
    private ConcurrentHashMap<String, User> users; // sessionId -> User
    private GameState state;
    private Problem currentProblem;
    private Integer selectedRating;
    private Instant gameStartTime;
    private String winnerId; // sessionId of the winner
    private String hostUsername; // Username of the host (persists across sessions)

    public GameRoom(String roomId) {
        this.roomId = roomId;
        this.users = new ConcurrentHashMap<>();
        this.state = GameState.WAITING;
    }

    /**
     * Adds a user to the room
     */
    public void addUser(User user) {
        users.put(user.getSessionId(), user);
    }

    /**
     * Removes a user from the room
     */
    public void removeUser(String sessionId) {
        users.remove(sessionId);
    }

    /**
     * Gets all users as a list
     */
    public List<User> getUserList() {
        return new ArrayList<>(users.values());
    }

    /**
     * Checks if the room is empty
     */
    public boolean isEmpty() {
        return users.isEmpty();
    }

    /**
     * Gets the host user
     */
    public User getHost() {
        return users.values().stream()
                .filter(User::isHost)
                .findFirst()
                .orElse(null);
    }

    public enum GameState {
        WAITING, // Waiting for players to join
        STARTED, // Game has started, users are solving
        FINISHED // Game finished, winner declared
    }
}
