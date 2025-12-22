package com.coderace.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for code submission request from frontend
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmitCodeRequest {

    private String code;
    private String language;
    private String problemId;
}
