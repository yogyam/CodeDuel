package com.coderace.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Registration request DTO with validation
 */
public record RegisterRequest(
                @Email(message = "Invalid email format") @NotBlank(message = "Email is required") String email,

                @NotBlank(message = "Password is required") @Size(min = 8, max = 100, message = "Password must be 8-100 characters") @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$", message = "Password must contain at least one uppercase letter, one lowercase letter, and one number") String password,

                @Pattern(regexp = "^[a-zA-Z0-9_-]{3,24}$", message = "Codeforces handle must be 3-24 alphanumeric characters") String codeforcesHandle // Optional
) {
}
