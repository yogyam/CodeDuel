package com.coderace.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for joining an existing game room
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JoinRoomRequest {
    private String roomId;
    private String codeforcesHandle;
}
