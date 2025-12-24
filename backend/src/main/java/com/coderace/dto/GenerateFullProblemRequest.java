package com.coderace.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to generate full problem after user selects a title
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenerateFullProblemRequest {
    private ProblemFilter filter;
    private ProblemTitleOption selectedTitle;
}
