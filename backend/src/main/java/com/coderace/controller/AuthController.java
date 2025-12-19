package com.coderace.controller;

import com.coderace.dto.AuthResponse;
import com.coderace.dto.LoginRequest;
import com.coderace.dto.RegisterRequest;
import com.coderace.entity.User;
import com.coderace.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for authentication endpoints
 */
@RestController
@RequestMapping("/api/auth")
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
            log.error("Login failed: {}", e.getMessage());

            // Return 404 for email not found to suggest signup
            if (e.getMessage().contains("Email not found")) {
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", e.getMessage()));
            }

            // Return 401 for invalid password
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", e.getMessage()));
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
    public ResponseEntity<Void> updateCodeforcesHandle(
            @AuthenticationPrincipal User user,
            @RequestParam String handle) {

        authService.updateCodeforcesHandle(user.getId(), handle);
        return ResponseEntity.ok().build();
    }
}
