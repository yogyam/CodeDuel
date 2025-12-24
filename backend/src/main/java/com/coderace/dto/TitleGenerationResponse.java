package com.coderace.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response from AI when generating problem titles (step 1)
 * Contains exactly 3 title options for user to choose from
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TitleGenerationResponse {
    private List<ProblemTitleOption> options;
}
