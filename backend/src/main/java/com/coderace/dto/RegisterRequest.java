package com.coderace.dto;

/**
 * Registration request DTO
 */
public record RegisterRequest(
        String email,
        String password,
        String codeforcesHandle // Optional
) {
}
