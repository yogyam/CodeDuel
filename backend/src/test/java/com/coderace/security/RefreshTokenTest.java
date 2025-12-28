package com.coderace.security;

import com.coderace.dto.RegisterRequest;
import com.coderace.service.RefreshTokenService;
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

/**
 * Integration tests for refresh token functionality
 * Tests token rotation, expiration, and multi-device logout
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class RefreshTokenTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RefreshTokenService refreshTokenService;

    private static final String TEST_EMAIL = "refreshtest@example.com";
    private static final String TEST_PASSWORD = "TestPass123";

    @Test
    public void testTokenRotation_OldTokenBecomesInvalid() throws Exception {
        // Register user
        RegisterRequest registerRequest = new RegisterRequest(TEST_EMAIL, TEST_PASSWORD);
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andReturn();

        Cookie oldRefreshToken = result.getResponse().getCookie("refresh_token");

        // Use refresh token once
        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                .cookie(oldRefreshToken))
                .andExpect(status().isOk())
                .andReturn();

        // Try to use old refresh token again - should fail
        mockMvc.perform(post("/api/auth/refresh")
                .cookie(oldRefreshToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid or expired refresh token"));
    }

    @Test
    public void testLogoutAll_RevokesAllTokens() throws Exception {
        // Register user
        RegisterRequest registerRequest = new RegisterRequest(TEST_EMAIL, TEST_PASSWORD);
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andReturn();

        Cookie authToken = result.getResponse().getCookie("auth_token");
        Cookie refreshToken = result.getResponse().getCookie("refresh_token");

        // Logout from all devices
        mockMvc.perform(post("/api/auth/logout-all")
                .cookie(authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out from all devices"));

        // Try to use refresh token - should fail
        mockMvc.perform(post("/api/auth/refresh")
                .cookie(refreshToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testMultipleRefreshTokens_CanCoexist() throws Exception {
        // Register user
        RegisterRequest registerRequest = new RegisterRequest(TEST_EMAIL, TEST_PASSWORD);
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        // Login from "device 1"
        MvcResult login1 = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new com.coderace.dto.LoginRequest(TEST_EMAIL, TEST_PASSWORD))))
                .andReturn();

        // Login from "device 2"
        MvcResult login2 = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new com.coderace.dto.LoginRequest(TEST_EMAIL, TEST_PASSWORD))))
                .andReturn();

        Cookie device1Refresh = login1.getResponse().getCookie("refresh_token");
        Cookie device2Refresh = login2.getResponse().getCookie("refresh_token");

        // Both refresh tokens should work
        mockMvc.perform(post("/api/auth/refresh")
                .cookie(device1Refresh))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/refresh")
                .cookie(device2Refresh))
                .andExpect(status().isOk());
    }

    @Test
    public void testRefreshToken_UpdatesBothCookies() throws Exception {
        // Register user
        RegisterRequest registerRequest = new RegisterRequest(TEST_EMAIL, TEST_PASSWORD);
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andReturn();

        Cookie oldRefreshToken = result.getResponse().getCookie("refresh_token");
        String oldRefreshValue = oldRefreshToken.getValue();

        // Refresh tokens
        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                .cookie(oldRefreshToken))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("auth_token"))
                .andExpect(cookie().exists("refresh_token"))
                .andReturn();

        Cookie newRefreshToken = refreshResult.getResponse().getCookie("refresh_token");

        // Verify new refresh token is different
        assert !oldRefreshValue.equals(newRefreshToken.getValue());
    }
}
