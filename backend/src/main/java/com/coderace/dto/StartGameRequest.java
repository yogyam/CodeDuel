package com.coderace.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for starting a game with problem filters
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StartGameRequest {
    private String description; // Free-form description of the desired problem
}
