package com.coderace.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response containing 3 problem title options for user selection
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenerateTitlesResponse {
    private List<ProblemTitleOption> options; // Always 3 options
}
