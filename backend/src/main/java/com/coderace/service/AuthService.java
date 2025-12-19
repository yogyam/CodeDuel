package com.coderace.service;

import com.coderace.entity.User;
import com.coderace.repository.UserRepository;
import com.coderace.security.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, JwtUtil jwtUtil, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Register new user with email and password
     * 
     * @param email            User email
     * @param password         Plain text password
     * @param codeforcesHandle Optional Codeforces handle
     * @return JWT token
     */
    @Transactional
    public String registerWithEmail(String email, String password, String codeforcesHandle) {
        log.info("Email registration attempt for: {}", email);

        // Check if email already exists
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Email already registered");
        }

        // Validate password
        validatePassword(password);

        // Hash password
        String hashedPassword = passwordEncoder.encode(password);

        // Create user
        User user = new User();
        user.setEmail(email.toLowerCase());
        user.setUsername(generateUsernameFromEmail(email));
        user.setPasswordHash(hashedPassword);
        user.setCodeforcesHandle(codeforcesHandle);
        user = userRepository.save(user);

        log.info("Created new user via email registration: {}", email);

        // Generate JWT
        return jwtUtil.generateToken(user.getId(), user.getEmail(), user.getUsername());
    }

    /**
     * Login user with email and password
     * 
     * @param email    User email
     * @param password Plain text password
     * @return JWT token
     */
    @Transactional(readOnly = true)
    public String loginWithEmail(String email, String password) {
        log.info("Email login attempt for: {}", email);

        // Find user by email
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new RuntimeException("Email not found"));

        // Check if user has password set
        if (user.getPasswordHash() == null) {
            throw new RuntimeException("This account was created with Google OAuth. Please use Google Sign In.");
        }

        // Verify password
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new RuntimeException("Invalid password");
        }

        log.info("Successful email login for: {}", email);

        // Generate JWT
        return jwtUtil.generateToken(user.getId(), user.getEmail(), user.getUsername());
    }

    /**
     * Validate password requirements
     * 
     * @param password Plain text password
     */
    private void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new RuntimeException("Password must be at least 8 characters long");
        }

        boolean hasLetter = false;
        boolean hasDigit = false;

        for (char c : password.toCharArray()) {
            if (Character.isLetter(c))
                hasLetter = true;
            if (Character.isDigit(c))
                hasDigit = true;
        }

        if (!hasLetter || !hasDigit) {
            throw new RuntimeException("Password must contain at least one letter and one number");
        }
    }

    /**
     * Handle Google OAuth login
     * Creates new user if doesn't exist, otherwise returns existing user
     * Also links Google account to existing email/password users
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
            log.info("Linked existing email/password user to Google OAuth: {}", email);
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
