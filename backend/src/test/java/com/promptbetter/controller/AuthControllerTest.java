package com.promptbetter.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.promptbetter.filters.JwtAuthFilter;
import com.promptbetter.service.AuthService;
import com.promptbetter.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for AuthController using MockMvc.
 *
 * KEY CONCEPTS:
 * - @WebMvcTest loads ONLY the web layer (controller + filters), not the full app.
 * - @MockBean creates a mock that lives in the Spring context (replaces the real bean).
 * - MockMvc lets us send fake HTTP requests and verify the responses.
 * - We disable security with the addFilters=false approach via @AutoConfigureMockMvc.
 */
@WebMvcTest(AuthController.class)       // Only load AuthController
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for simplicity
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc; // Simulates HTTP requests

    @MockBean
    private AuthService authService;

    // --- Beans that SecurityConfig / JwtAuthFilter need to start the context ---
    // Even though we disabled filters, Spring still instantiates SecurityConfig,
    // which depends on JwtAuthFilter, which depends on these two.
    // We mock them so the context can load successfully.
    @MockBean
    private JwtAuthFilter jwtAuthFilter;
    @MockBean
    private JwtUtil jwtUtil;
    @MockBean
    private UserDetailsService userDetailsService;

    private final ObjectMapper objectMapper = new ObjectMapper(); // Converts objects to JSON

    @Test
    void register_success_shouldReturn200() throws Exception {
        // GIVEN: authService.register() returns a token map
        when(authService.register("Alice", "alice@test.com", "pass123"))
                .thenReturn(Map.of("token", "jwt-token", "name", "Alice",
                        "email", "alice@test.com", "id", 1L));

        // WHEN + THEN: POST to /api/auth/register should return 200 with the token
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "Alice", "email", "alice@test.com", "password", "pass123"))))
                .andExpect(status().isOk())                     // HTTP 200
                .andExpect(jsonPath("$.token").value("jwt-token"))  // Check JSON field
                .andExpect(jsonPath("$.name").value("Alice"));
    }

    @Test
    void register_duplicateEmail_shouldReturn400() throws Exception {
        // GIVEN: authService throws on duplicate email
        when(authService.register("Alice", "alice@test.com", "pass123"))
                .thenThrow(new RuntimeException("Email already in use"));

        // WHEN + THEN: should return 400 with error message
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "Alice", "email", "alice@test.com", "password", "pass123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Email already in use"));
    }

    @Test
    void login_success_shouldReturn200() throws Exception {
        when(authService.login("alice@test.com", "pass123"))
                .thenReturn(Map.of("token", "jwt-token", "name", "Alice",
                        "email", "alice@test.com", "id", 1L));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", "alice@test.com", "password", "pass123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void login_wrongPassword_shouldReturn400() throws Exception {
        when(authService.login("alice@test.com", "wrong"))
                .thenThrow(new RuntimeException("Invalid password"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", "alice@test.com", "password", "wrong"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid password"));
    }
}
