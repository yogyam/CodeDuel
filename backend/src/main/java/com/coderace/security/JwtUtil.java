package com.coderace.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for JWT token generation and validation
 */
@Component
@Slf4j
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    @Value("${jwt.cookie.name}")
    private String cookieName;

    @Value("${jwt.cookie.http-only}")
    private boolean httpOnly;

    @Value("${jwt.cookie.secure}")
    private boolean secure;

    @Value("${jwt.cookie.same-site}")
    private String sameSite;

    @Value("${jwt.cookie.max-age}")
    private int maxAge;

    @Value("${jwt.refresh-cookie.name}")
    private String refreshCookieName;

    @Value("${jwt.refresh-cookie.max-age}")
    private int refreshCookieMaxAge;

    /**
     * Validates JWT secret on application startup
     * Ensures the secret is at least 256 bits (32 bytes) for HS256 algorithm
     */
    @PostConstruct
    public void validateSecret() {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            int actualLength = secret != null ? secret.getBytes(StandardCharsets.UTF_8).length : 0;
            throw new IllegalStateException(
                    String.format(
                            "JWT_SECRET must be at least 256 bits (32 characters). " +
                                    "Current length: %d bytes. " +
                                    "Please update your JWT_SECRET environment variable.",
                            actualLength));
        }
        log.info("JWT secret validated successfully ({} bytes)", secret.getBytes(StandardCharsets.UTF_8).length);
    }

    /**
     * Generates signing key from secret
     */
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generate JWT token for a user
     * 
     * @param userId   User's database ID
     * @param email    User's email
     * @param username User's username
     * @return JWT token string
     */
    public String generateToken(Long userId, String email, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("email", email);
        claims.put("username", username);

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        String token = Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();

        log.debug("Generated JWT token for user: {}", email);
        return token;
    }

    /**
     * Validate JWT token
     * 
     * @param token JWT token
     * @return true if valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extract user ID from token
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getClaims(token);
        return claims.get("userId", Long.class);
    }

    /**
     * Extract email from token
     */
    public String getEmailFromToken(String token) {
        Claims claims = getClaims(token);
        return claims.getSubject();
    }

    /**
     * Extract username from token
     */
    public String getUsernameFromToken(String token) {
        Claims claims = getClaims(token);
        return claims.get("username", String.class);
    }

    /**
     * Get all claims from token
     */
    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Check if token is expired
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = getClaims(token).getExpiration();
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Generate JWT token as an httpOnly cookie for XSS protection
     * 
     * @param userId   User's database ID
     * @param email    User's email
     * @param username User's username
     * @return Cookie object with JWT and security flags set
     */
    public jakarta.servlet.http.Cookie generateTokenCookie(Long userId, String email, String username) {
        String token = generateToken(userId, email, username);

        jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie(cookieName, token);
        cookie.setHttpOnly(httpOnly);
        cookie.setSecure(secure);
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);

        // SameSite attribute (not directly supported in Cookie class, must be set in
        // response)
        // Note: Will be added via response header in controller

        log.debug("Generated JWT cookie for user: {}", email);
        return cookie;
    }

    /**
     * Create a cookie to clear the authentication token (for logout)
     * 
     * @return Cookie with expired maxAge to clear client-side cookie
     */
    public jakarta.servlet.http.Cookie clearTokenCookie() {
        jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie(cookieName, null);
        cookie.setHttpOnly(httpOnly);
        cookie.setSecure(secure);
        cookie.setPath("/");
        cookie.setMaxAge(0); // Expire immediately

        log.debug("Generated cookie to clear JWT token");
        return cookie;
    }

    /**
     * Get the cookie name configured for JWT
     * 
     * @return Cookie name
     */
    public String getCookieName() {
        return cookieName;
    }

    /**
     * Generate a refresh token cookie
     * 
     * @param refreshToken The refresh token value
     * @return Cookie configured for refresh token
     */
    public jakarta.servlet.http.Cookie generateRefreshTokenCookie(String refreshToken) {
        jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie(refreshCookieName, refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(secure);
        cookie.setPath("/");
        cookie.setMaxAge(refreshCookieMaxAge);

        log.debug("Generated refresh token cookie");
        return cookie;
    }

    /**
     * Clear refresh token cookie (for logout)
     * 
     * @return Cookie with expired maxAge
     */
    public jakarta.servlet.http.Cookie clearRefreshTokenCookie() {
        jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie(refreshCookieName, null);
        cookie.setHttpOnly(true);
        cookie.setSecure(secure);
        cookie.setPath("/");
        cookie.setMaxAge(0);

        log.debug("Cleared refresh token cookie");
        return cookie;
    }
}
