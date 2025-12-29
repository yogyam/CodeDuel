package com.coderace.config;

import com.coderace.entity.User;
import com.coderace.security.JwtUtil;
import com.coderace.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;

/**
 * OAuth2 success handler for Google authentication
 * Sets JWT as httpOnly cookie and redirects to frontend
 */
@Configuration
@Slf4j
public class OAuth2SuccessHandler {

    @Bean
    public AuthenticationSuccessHandler oauth2AuthenticationSuccessHandler(
            AuthService authService,
            JwtUtil jwtUtil,
            @Value("${frontend.url}") String frontendUrl) {

        return (HttpServletRequest request, HttpServletResponse response, Authentication authentication) -> {
            try {
                OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
                String email = oauth2User.getAttribute("email");

                log.info("OAuth2 success for email: {}", email);

                // Process Google OAuth login (creates user if doesn't exist)
                String token = authService.handleGoogleLogin(oauth2User);

                // Get the user to generate cookie with proper info
                User user = authService.getUserByEmail(email);

                // Set JWT as httpOnly cookie
                Cookie cookie = jwtUtil.generateTokenCookie(user.getId(), user.getEmail(), user.getUsername());
                response.addCookie(cookie);

                log.info("Set httpOnly cookie for OAuth user: {}", email);

                // Redirect to frontend dashboard with token in URL (for cross-domain cookie
                // workaround)
                // The frontend will extract the token and store it in localStorage
                String redirectUrl = frontendUrl + "/dashboard?token=" + token;
                response.sendRedirect(redirectUrl);

            } catch (Exception e) {
                log.error("OAuth2 authentication error: {}", e.getMessage(), e);
                String errorUrl = frontendUrl + "/?error=oauth_failed";
                response.sendRedirect(errorUrl);
            }
        };
    }
}
