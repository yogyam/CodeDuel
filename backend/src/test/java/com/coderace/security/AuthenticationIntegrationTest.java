package com.coderace.security;

import com.coderace.dto.LoginRequest;
import com.coderace.dto.RegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.Cookie;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for authentication and authorization
 * Tests cookie-based JWT authentication, validation, and security headers
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class AuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "TestPass123";

    @Test
    public void testRegisterUser_Success() throws Exception {
        RegisterRequest request = new RegisterRequest(TEST_EMAIL, TEST_PASSWORD);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("auth_token"))
                .andExpect(cookie().exists("refresh_token"))
                .andExpect(cookie().httpOnly("auth_token", true))
                .andExpect(cookie().httpOnly("refresh_token", true))
                .andExpect(jsonPath("$.user.email").value(TEST_EMAIL));
    }

    @Test
    public void testRegisterUser_InvalidEmail() throws Exception {
        RegisterRequest request = new RegisterRequest("invalid-email", TEST_PASSWORD);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email").exists());
    }

    @Test
    public void testRegisterUser_DuplicateEmail() throws Exception {
        RegisterRequest request = new RegisterRequest(TEST_EMAIL, TEST_PASSWORD);

        // Register first time
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Try to register again with same email
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    public void testLogin_Success() throws Exception {
        // Register user first
        RegisterRequest registerRequest = new RegisterRequest(TEST_EMAIL, TEST_PASSWORD);
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        // Login
        LoginRequest loginRequest = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("auth_token"))
                .andExpect(cookie().exists("refresh_token"))
                .andExpect(jsonPath("$.user.email").value(TEST_EMAIL));
    }

    @Test
    public void testLogin_InvalidCredentials() throws Exception {
        LoginRequest loginRequest = new LoginRequest("nonexistent@example.com", "wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    public void testGetCurrentUser_Authenticated() throws Exception {
        // Register and get cookies
        RegisterRequest registerRequest = new RegisterRequest(TEST_EMAIL, TEST_PASSWORD);
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andReturn();

        Cookie authCookie = result.getResponse().getCookie("auth_token");

        // Access /me endpoint with cookie
        mockMvc.perform(get("/api/auth/me")
                .cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(TEST_EMAIL))
                .andExpect(jsonPath("$.username").exists());
    }

    @Test
    public void testGetCurrentUser_Unauthenticated() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testGetCurrentUser_InvalidToken() throws Exception {
        Cookie invalidCookie = new Cookie("auth_token", "invalid.jwt.token");

        mockMvc.perform(get("/api/auth/me")
                .cookie(invalidCookie))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testLogout_Success() throws Exception {
        // Register first
        RegisterRequest registerRequest = new RegisterRequest(TEST_EMAIL, TEST_PASSWORD);
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andReturn();

        Cookie authCookie = result.getResponse().getCookie("auth_token");

        // Logout
        mockMvc.perform(post("/api/auth/logout")
                .cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge("auth_token", 0))
                .andExpect(cookie().maxAge("refresh_token", 0))
                .andExpect(jsonPath("$.message").value("Logged out successfully"));
    }

    @Test
    public void testRefreshToken_Success() throws Exception {
        // Register and get cookies
        RegisterRequest registerRequest = new RegisterRequest(TEST_EMAIL, TEST_PASSWORD);
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andReturn();

        Cookie refreshCookie = result.getResponse().getCookie("refresh_token");

        // Refresh token
        mockMvc.perform(post("/api/auth/refresh")
                .cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("auth_token"))
                .andExpect(cookie().exists("refresh_token"))
                .andExpect(jsonPath("$.message").value("Token refreshed successfully"));
    }

    @Test
    public void testRefreshToken_MissingToken() throws Exception {
        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Refresh token not found"));
    }

    @Test
    public void testRefreshToken_InvalidToken() throws Exception {
        Cookie invalidRefreshCookie = new Cookie("refresh_token", "invalid_token");

        mockMvc.perform(post("/api/auth/refresh")
                .cookie(invalidRefreshCookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(containsString("Invalid or expired")));
    }

    @Test
    public void testSecurityHeaders_Present() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(header().exists("X-Content-Type-Options"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().exists("X-Frame-Options"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().exists("Content-Security-Policy"));
    }

    @Test
    public void testCorsHeaders_Present() throws Exception {
        mockMvc.perform(options("/api/auth/me")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "GET"))
                .andExpect(header().exists("Access-Control-Allow-Origin"))
                .andExpect(header().exists("Access-Control-Allow-Credentials"));
    }
}
