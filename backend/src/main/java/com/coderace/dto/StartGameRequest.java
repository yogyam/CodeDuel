package com.coderace.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request to start a game
 * Can either provide description for on-the-fly generation,
 * or problemId for pre-generated problem
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StartGameRequest {
    private String description; // For legacy direct generation
    private String problemId; // For two-step generation with pre-generated problem
}
