package com.coderace.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single problem title option in the two-step generation flow
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProblemTitleOption {
    private String title;
    private String briefDescription; // 1-2 sentences
    private String concept; // e.g., "0/1 Knapsack variant"
}
