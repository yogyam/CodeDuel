package com.coderace.service;

import com.coderace.entity.RefreshToken;
import com.coderace.repository.RefreshTokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * Service for managing refresh tokens
 * Handles token creation, validation, rotation, and revocation
 */
@Service
@Slf4j
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${jwt.refresh-token.expiration:604800000}") // 7 days in milliseconds
    private Long refreshTokenExpiration;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    /**
     * Create a new refresh token for a user
     */
    @Transactional
    public String createRefreshToken(Long userId) {
        String token = generateSecureToken();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(token);
        refreshToken.setUserId(userId);
        refreshToken.setExpiresAt(LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000));
        refreshToken.setRevoked(false);

        refreshTokenRepository.save(refreshToken);
        log.info("Created refresh token for user {}", userId);

        return token;
    }

    /**
     * Validate and rotate a refresh token
     * Returns new refresh token if valid, null if invalid/expired
     */
    @Transactional
    public String rotateRefreshToken(String oldToken) {
        return refreshTokenRepository.findByToken(oldToken)
                .filter(token -> !token.getRevoked() && !token.isExpired())
                .map(token -> {
                    // Revoke old token
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);

                    // Create new token
                    String newToken = createRefreshToken(token.getUserId());
                    log.info("Rotated refresh token for user {}", token.getUserId());
                    return newToken;
                })
                .orElse(null);
    }

    /**
     * Get user ID from a valid refresh token
     */
    @Transactional(readOnly = true)
    public Long getUserIdFromToken(String token) {
        return refreshTokenRepository.findByToken(token)
                .filter(rt -> !rt.getRevoked() && !rt.isExpired())
                .map(RefreshToken::getUserId)
                .orElse(null);
    }

    /**
     * Revoke all refresh tokens for a user (logout all devices)
     */
    @Transactional
    public void revokeAllUserTokens(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
        log.info("Revoked all refresh tokens for user {}", userId);
    }

    /**
     * Cleanup expired and revoked tokens (scheduled daily)
     */
    @Scheduled(cron = "0 0 2 * * *") // Run at 2 AM every day
    @Transactional
    public void cleanupExpiredTokens() {
        refreshTokenRepository.deleteExpiredAndRevoked(LocalDateTime.now());
        log.info("Cleaned up expired and revoked refresh tokens");
    }

    /**
     * Generate a cryptographically secure random token
     */
    private String generateSecureToken() {
        byte[] randomBytes = new byte[64];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
