package com.yogesh.ragassistant.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yogesh.ragassistant.dto.request.LoginRequest;
import com.yogesh.ragassistant.dto.request.RegisterRequest;
import com.yogesh.ragassistant.dto.response.AuthResponse;
import com.yogesh.ragassistant.security.CustomUserDetailsService;
import com.yogesh.ragassistant.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("POST /api/auth/register should return 201 Created on valid input")
    void testRegisterSuccess() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("newuser@company.com")
                .password("Password@123")
                .fullName("New User")
                .role("ROLE_USER")
                .build();

        AuthResponse authResponse = AuthResponse.builder()
                .userId(UUID.randomUUID())
                .email("newuser@company.com")
                .fullName("New User")
                .role("ROLE_USER")
                .authType("HTTP Basic")
                .message("User registered successfully")
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("newuser@company.com"))
                .andExpect(jsonPath("$.data.authType").value("HTTP Basic"));
    }

    @Test
    @DisplayName("POST /api/auth/login should return 200 OK on valid credentials")
    void testLoginSuccess() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("user@company.com")
                .password("Password@123")
                .build();

        AuthResponse authResponse = AuthResponse.builder()
                .userId(UUID.randomUUID())
                .email("user@company.com")
                .fullName("Enterprise User")
                .role("ROLE_USER")
                .authType("HTTP Basic")
                .message("Authentication successful")
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.authType").value("HTTP Basic"));
    }
}
