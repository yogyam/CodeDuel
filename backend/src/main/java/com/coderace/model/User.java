package com.coderace.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a user in a game room
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private String handle; // User handle/identifier
    private String sessionId;
    private boolean isHost;
    private String username; // Authenticated username from JWT
    private UserStatus status;

    public User(String handle, String sessionId, boolean isHost, String username) {
        this.handle = handle;
        this.sessionId = sessionId;
        this.isHost = isHost;
        this.username = username;
        this.status = UserStatus.WAITING;
    }

    public enum UserStatus {
        WAITING,
        SOLVING,
        WON
    }
}
