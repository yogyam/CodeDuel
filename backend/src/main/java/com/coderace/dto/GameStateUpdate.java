package com.coderace.dto;

import com.coderace.model.GameRoom;
import com.coderace.model.Problem;
import com.coderace.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for sending game state updates to clients via WebSocket
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameStateUpdate {
    private String roomId;
    private GameRoom.GameState state;
    private List<User> users;
    private Problem problem;
    private String winnerId;
    private String message;
    
    public GameStateUpdate(GameRoom room, String message) {
        this.roomId = room.getRoomId();
        this.state = room.getState();
        this.users = room.getUserList();
        this.problem = room.getCurrentProblem();
        this.winnerId = room.getWinnerId();
        this.message = message;
    }
}
