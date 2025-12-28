package com.coderace.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to create a new game room
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateRoomRequest {
    private String handle; // User handle
}
