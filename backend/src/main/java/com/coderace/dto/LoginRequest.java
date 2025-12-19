package com.coderace.dto;

/**
 * Login request DTO
 */
public record LoginRequest(
        String email,
        String password) {
}
