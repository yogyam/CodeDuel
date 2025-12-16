package com.coderace.service;

import com.coderace.entity.User;
import com.coderace.repository.UserRepository;
import com.coderace.security.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for handling user authentication
 */
@Service
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Handle Google OAuth login
     * Creates new user if doesn't exist, otherwise returns existing user
     * 
     * @param oauth2User OAuth2 user details from Google
     * @return JWT token
     */
    @Transactional
    public String handleGoogleLogin(OAuth2User oauth2User) {
        String email = oauth2User.getAttribute("email");
        String googleId = oauth2User.getAttribute("sub"); // Google's unique user ID

        log.info("Google OAuth login attempt for email: {}", email);

        // Check if user already exists by Google ID
        User user = userRepository.findByGoogleId(googleId)
                .or(() -> userRepository.findByEmail(email))
                .orElse(null);

        if (user == null) {
            // Create new user
            user = new User();
            user.setEmail(email);
            user.setUsername(generateUsernameFromEmail(email));
            user.setGoogleId(googleId);
            user = userRepository.save(user);
            log.info("Created new user from Google OAuth: {}", email);
        } else if (user.getGoogleId() == null) {
            // User exists with email but not linked to Google - link it
            user.setGoogleId(googleId);
            user = userRepository.save(user);
            log.info("Linked existing user to Google OAuth: {}", email);
        }

        // Generate JWT token
        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getUsername());
        log.info("Generated JWT token for user: {}", email);

        return token;
    }

    /**
     * Get user by ID
     */
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    /**
     * Get user by email
     */
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    /**
     * Generate username from email
     * Example: john.doe@gmail.com -> johndoe
     */
    private String generateUsernameFromEmail(String email) {
        String baseUsername = email.split("@")[0].replaceAll("[^a-zA-Z0-9]", "").toLowerCase();

        // Check if username exists, append number if it does
        String username = baseUsername;
        int counter = 1;
        while (userRepository.existsByUsername(username)) {
            username = baseUsername + counter;
            counter++;
        }

        return username;
    }

    /**
     * Update user's Codeforces handle
     */
    @Transactional
    public void updateCodeforcesHandle(Long userId, String codeforcesHandle) {
        User user = getUserById(userId);
        user.setCodeforcesHandle(codeforcesHandle);
        userRepository.save(user);
        log.info("Updated Codeforces handle for user {}: {}", userId, codeforcesHandle);
    }
}
