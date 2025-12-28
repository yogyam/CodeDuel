package com.coderace.controller;

import com.coderace.dto.*;
import com.coderace.entity.User;
import com.coderace.security.JwtUtil;
import com.coderace.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    private final JwtUtil jwtUtil;
    private final com.coderace.service.RefreshTokenService refreshTokenService;

    public AuthController(AuthService authService, JwtUtil jwtUtil,
            com.coderace.service.RefreshTokenService refreshTokenService) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
    }

    /**
     * Register new user with email and password
     * Sets JWT token as httpOnly cookie
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response) {
        try {
            String token = authService.registerWithEmail(
                    request.email(),
                    request.password());

            User user = authService.getUserByEmail(request.email());

            // Set access token cookie
            jakarta.servlet.http.Cookie accessCookie = jwtUtil.generateTokenCookie(
                    user.getId(), user.getEmail(), user.getUsername());
            response.addCookie(accessCookie);

            // Create and set refresh token cookie
            String refreshToken = refreshTokenService.createRefreshToken(user.getId());
            jakarta.servlet.http.Cookie refreshCookie = jwtUtil.generateRefreshTokenCookie(refreshToken);
            response.addCookie(refreshCookie);

            // Return user info only (no token in body)
            AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
                    user.getId(),
                    user.getEmail(),
                    user.getUsername());

            return ResponseEntity.ok(Map.of("user", userInfo));
        } catch (RuntimeException e) {
            log.error("Registration failed: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Login user with email and password
     * Sets JWT token as httpOnly cookie
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {
        try {
            String token = authService.loginWithEmail(
                    request.email(),
                    request.password());

            User user = authService.getUserByEmail(request.email());

            // Set JWT as httpOnly cookie
            Cookie cookie = jwtUtil.generateTokenCookie(user.getId(), user.getEmail(), user.getUsername());
            response.addCookie(cookie);

            // Return user info only (no token in body)
            AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
                    user.getId(),
                    user.getEmail(),
                    user.getUsername());

            return ResponseEntity.ok(Map.of("user", userInfo));
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
     * Logout - clear the JWT cookie
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response, @AuthenticationPrincipal User user) {
        // Clear both access and refresh token cookies
        response.addCookie(jwtUtil.clearTokenCookie());
        response.addCookie(jwtUtil.clearRefreshTokenCookie());

        // Revoke the specific refresh token if user is authenticated
        if (user != null) {
            // Note: specific token revocation would require passing the token
            // For now, we just clear cookies client-side
        }

        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    /**
     * Get current authenticated user info
     * User is injected from SecurityContext (populated by JWT filter)
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Not authenticated"));
        }

        AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
                user.getId(),
                user.getEmail(),
                user.getUsername());
        return ResponseEntity.ok(userInfo);
    }

    /**
     * Refresh access token using refresh token
     * Implements token rotation - old refresh token is revoked, new one issued
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(
            @CookieValue(value = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response) {

        if (refreshToken == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Refresh token not found"));
        }

        try {
            // Get user ID from refresh token
            Long userId = refreshTokenService.getUserIdFromToken(refreshToken);
            if (userId == null) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Invalid or expired refresh token"));
            }

            // Get user details
            User user = authService.getUserById(userId);

            // Generate new access token
            jakarta.servlet.http.Cookie accessCookie = jwtUtil.generateTokenCookie(
                    user.getId(), user.getEmail(), user.getUsername());
            response.addCookie(accessCookie);

            // Rotate refresh token
            String newRefreshToken = refreshTokenService.rotateRefreshToken(refreshToken);
            if (newRefreshToken != null) {
                jakarta.servlet.http.Cookie refreshCookie = jwtUtil.generateRefreshTokenCookie(newRefreshToken);
                response.addCookie(refreshCookie);
            } else {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Failed to rotate refresh token"));
            }

            return ResponseEntity.ok(Map.of("message", "Token refreshed successfully"));
        } catch (Exception e) {
            log.error("Error refreshing token", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to refresh token"));
        }
    }

    /**
     * Logout from all devices - revokes all refresh tokens for the user
     */
    @PostMapping("/logout-all")
    public ResponseEntity<?> logoutAll(
            @AuthenticationPrincipal User user,
            HttpServletResponse response) {

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Not authenticated"));
        }

        // Revoke all refresh tokens
        refreshTokenService.revokeAllUserTokens(user.getId());

        // Clear cookies
        response.addCookie(jwtUtil.clearTokenCookie());
        response.addCookie(jwtUtil.clearRefreshTokenCookie());

        return ResponseEntity.ok(Map.of("message", "Logged out from all devices"));
    }
}
