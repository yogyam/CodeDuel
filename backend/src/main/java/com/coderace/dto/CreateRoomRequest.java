package com.coderace.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a new game room
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateRoomRequest {
    private String codeforcesHandle;
}
