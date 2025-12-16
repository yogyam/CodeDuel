package com.coderace.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import com.coderace.service.AuthService;
import com.coderace.security.JwtUtil;

/**
 * Custom OAuth2 success handler
 * Generates JWT and redirects to frontend with token
 */
@Configuration
public class OAuth2SuccessHandler {

    @Value("${FRONTEND_URL:http://localhost:5173}")
    private String frontendUrl;

    @Bean
    public AuthenticationSuccessHandler oauth2AuthenticationSuccessHandler(
            AuthService authService, JwtUtil jwtUtil) {

        return (request, response, authentication) -> {
            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();

            // Generate JWT token
            String token = authService.handleGoogleLogin(oauth2User);

            // Redirect to frontend with token
            String redirectUrl = frontendUrl + "?token=" + token;
            response.sendRedirect(redirectUrl);
        };
    }
}
