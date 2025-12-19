package com.coderace.config;

import com.coderace.security.JwtAuthenticationFilter;
import com.coderace.security.RestAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

/**
 * Spring Security configuration
 * Configures OAuth2 login, JWT authentication, and endpoint security
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtAuthenticationFilter;
        private final AuthenticationSuccessHandler oauth2SuccessHandler;
        private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;

        public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                        AuthenticationSuccessHandler oauth2SuccessHandler,
                        RestAuthenticationEntryPoint restAuthenticationEntryPoint) {
                this.jwtAuthenticationFilter = jwtAuthenticationFilter;
                this.oauth2SuccessHandler = oauth2SuccessHandler;
                this.restAuthenticationEntryPoint = restAuthenticationEntryPoint;
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                // Disable CSRF since we're using JWT
                                .csrf(csrf -> csrf.disable())

                                // Configure CORS (will use existing CorsConfig)
                                .cors(cors -> cors.configure(http))

                                // Set session management to stateless
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                                // Configure authorization
                                .authorizeHttpRequests(auth -> auth
                                                // Public auth endpoints (login, register)
                                                .requestMatchers("/api/auth/login", "/api/auth/register").permitAll()
                                                .requestMatchers("/ws/**").permitAll()
                                                .requestMatchers("/login/oauth2/**").permitAll()
                                                .requestMatchers("/oauth2/**").permitAll()
                                                // All other endpoints require authentication (including /api/auth/me)
                                                .anyRequest().authenticated())

                                // Configure exception handling - return 401 instead of redirecting
                                .exceptionHandling(exception -> exception
                                                .authenticationEntryPoint(restAuthenticationEntryPoint))

                                // Configure OAuth2 login with custom success handler
                                .oauth2Login(oauth2 -> oauth2
                                                .successHandler(oauth2SuccessHandler))

                                // Add JWT filter
                                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }
}
