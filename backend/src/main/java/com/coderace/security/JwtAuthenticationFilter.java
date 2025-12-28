package com.coderace.security;

import com.coderace.entity.User;
import com.coderace.service.UserCacheService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT Authentication Filter
 * Extracts and validates JWT from Authorization header
 */
@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserCacheService userCacheService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserCacheService userCacheService) {
        this.jwtUtil = jwtUtil;
        this.userCacheService = userCacheService;
    }

    @Override
    protected void doFilterInternal(
            @org.springframework.lang.NonNull HttpServletRequest request,
            @org.springframework.lang.NonNull HttpServletResponse response,
            @org.springframework.lang.NonNull FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String jwt = extractJwtFromRequest(request);

            if (jwt != null && jwtUtil.validateToken(jwt)) {
                Long userId = jwtUtil.getUserIdFromToken(jwt);
                String email = jwtUtil.getEmailFromToken(jwt);

                // Load user from cache (fallback to database if cache miss)
                User user = userCacheService.getUserById(userId);

                if (user == null) {
                    log.warn("User {} not found for valid JWT", userId);
                    filterChain.doFilter(request, response);
                    return;
                }

                // Create Spring Security authentication
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        Collections.emptyList());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Set authentication in SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("Set authentication for user: {}", email);
            }
        } catch (Exception e) {
            log.error("Could not set user authentication: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extract JWT token from cookies or Authorization header
     * Priority: cookies first (new flow), then Authorization header (backward
     * compatibility)
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        // PRIORITY 1: Check cookies for JWT (httpOnly cookie from new auth flow)
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (jwtUtil.getCookieName().equals(cookie.getName())) {
                    log.debug("Found JWT in cookie");
                    return cookie.getValue();
                }
            }
        }

        // PRIORITY 2: Fallback to Authorization header (backward compatibility)
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            log.debug("Found JWT in Authorization header");
            return bearerToken.substring(7);
        }

        return null;
    }
}
