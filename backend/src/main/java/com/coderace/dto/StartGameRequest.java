package com.coderace.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for starting a game with selected difficulty rating
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StartGameRequest {
    private Integer rating;
}
