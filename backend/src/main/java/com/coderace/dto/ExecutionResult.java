package com.coderace.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for code execution result from Piston API
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionResult {

    private String output;
    private String error;
    private int exitCode;
    private long executionTime; // milliseconds

    public boolean isSuccess() {
        return exitCode == 0 && (error == null || error.isEmpty());
    }

    public String getCleanOutput() {
        return output != null ? output.trim() : "";
    }
}
