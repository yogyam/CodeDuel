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
    private String codeforcesHandle;
    private String sessionId;
    private boolean isHost;
    private UserStatus status;
    
    public User(String codeforcesHandle, String sessionId, boolean isHost) {
        this.codeforcesHandle = codeforcesHandle;
        this.sessionId = sessionId;
        this.isHost = isHost;
        this.status = UserStatus.WAITING;
    }
    
    public enum UserStatus {
        WAITING,
        SOLVING,
        WON
    }
}
