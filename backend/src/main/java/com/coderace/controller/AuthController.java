package com.coderace.controller;

import com.coderace.dto.AuthResponse;
import com.coderace.dto.LoginRequest;
import com.coderace.dto.RegisterRequest;
import com.coderace.entity.User;
import com.coderace.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for authentication endpoints
 */
@RestController
@RequestMapping("/api/auth")
@Validated
@Slf4j
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Register new user with email and password
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            String token = authService.registerWithEmail(
                    request.email(),
                    request.password(),
                    request.codeforcesHandle());

            User user = authService.getUserByEmail(request.email());

            AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
                    user.getId(),
                    user.getEmail(),
                    user.getUsername(),
                    user.getCodeforcesHandle());

            return ResponseEntity.ok(new AuthResponse(token, userInfo));
        } catch (RuntimeException e) {
            log.error("Registration failed: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Login user with email and password
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            String token = authService.loginWithEmail(
                    request.email(),
                    request.password());

            User user = authService.getUserByEmail(request.email());

            AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
                    user.getId(),
                    user.getEmail(),
                    user.getUsername(),
                    user.getCodeforcesHandle());

            return ResponseEntity.ok(new AuthResponse(token, userInfo));
        } catch (RuntimeException e) {
            // Log the actual error for debugging (not sent to client)
            log.error("Login failed for email {}: {}", request.email(), e.getMessage());

            // SECURITY: Always return generic message to prevent user enumeration
            // Don't reveal whether email exists or password is wrong
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid email or password"));
        }
    }

    /**
     * Get current authenticated user info
     */
    @GetMapping("/me")
    public ResponseEntity<AuthResponse.UserInfo> getCurrentUser(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getCodeforcesHandle());

        return ResponseEntity.ok(userInfo);
    }

    /**
     * Update Codeforces handle for current user
     */
    @PutMapping("/codeforces-handle")
    public ResponseEntity<?> updateCodeforcesHandle(
            @AuthenticationPrincipal User user,
            @RequestParam @Pattern(regexp = "^[a-zA-Z0-9_-]{3,24}$", message = "Codeforces handle must be 3-24 characters (alphanumeric, underscore, or hyphen only)") String handle) {

        authService.updateCodeforcesHandle(user.getId(), handle);
        return ResponseEntity.ok().build();
    }
}
