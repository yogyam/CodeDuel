package com.coderace.controller;

import com.coderace.dto.AuthResponse;
import com.coderace.entity.User;
import com.coderace.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

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
