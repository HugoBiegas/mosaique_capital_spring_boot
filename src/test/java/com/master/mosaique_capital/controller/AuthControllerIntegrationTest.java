// src/test/java/com/master/mosaique_capital/controller/AuthControllerIntegrationTest.java
package com.master.mosaique_capital.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.master.mosaique_capital.dto.auth.JwtResponse;
import com.master.mosaique_capital.dto.auth.LoginRequest;
import com.master.mosaique_capital.dto.auth.SignupRequest;
import com.master.mosaique_capital.entity.User;
import com.master.mosaique_capital.enums.Role;
import com.master.mosaique_capital.repository.UserRepository;
import com.master.mosaique_capital.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    private SignupRequest signupRequest;
    private LoginRequest loginRequest;
    private String jwtToken;

    @BeforeEach
    void setUp() {
        // Nettoyer la base de données
        userRepository.deleteAll();

        // Initialiser la requête d'inscription
        signupRequest = new SignupRequest();
        signupRequest.setUsername("testuser");
        signupRequest.setEmail("test@example.com");
        signupRequest.setPassword("Password123!");

        // Initialiser la requête de connexion
        loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("Password123!");

        // Créer un utilisateur de test pour les tests qui nécessitent un utilisateur existant
        User testUser = new User();
        testUser.setUsername("existinguser");
        testUser.setEmail("existing@example.com");
        testUser.setPassword(passwordEncoder.encode("Password123!"));
        testUser.setRoles(Collections.singleton(Role.ROLE_USER));
        testUser.setActive(true);

        userRepository.save(testUser);

        // Générer un token JWT pour les tests qui nécessitent une authentification
        jwtToken = "Bearer " + tokenProvider.createToken("existinguser");
    }

    @Test
    void signup_ShouldRegisterUser_WhenInputIsValid() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Utilisateur enregistré avec succès"));

        // Vérifier que l'utilisateur a été créé
        assertTrue(userRepository.existsByUsername("testuser"));
        assertTrue(userRepository.existsByEmail("test@example.com"));
    }

    @Test
    void signup_ShouldReturnError_WhenUsernameExists() throws Exception {
        // Given - Créer un utilisateur avec le même nom d'utilisateur
        User existingUser = new User();
        existingUser.setUsername("testuser");
        existingUser.setEmail("other@example.com");
        existingUser.setPassword(passwordEncoder.encode("Password123!"));
        existingUser.setRoles(Collections.singleton(Role.ROLE_USER));
        userRepository.save(existingUser);

        // When & Then
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ce nom d'utilisateur est déjà utilisé"));
    }

    @Test
    void login_ShouldReturnJwtToken_WhenCredentialsAreValid() throws Exception {
        // Given - Créer un utilisateur pour le test de connexion
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword(passwordEncoder.encode("Password123!"));
        user.setRoles(Collections.singleton(Role.ROLE_USER));
        user.setActive(true);
        userRepository.save(user);

        // When & Then
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        // Vérifier la réponse
        JwtResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), JwtResponse.class);
        assertNotNull(response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertEquals("testuser", response.getUsername());
    }

    @Test
    void login_ShouldReturnError_WhenCredentialsAreInvalid() throws Exception {
        // Given
        LoginRequest invalidRequest = new LoginRequest();
        invalidRequest.setUsername("testuser");
        invalidRequest.setPassword("wrongpassword");

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void setupMfa_ShouldReturnMfaSecret_WhenUserIsAuthenticated() throws Exception {
        // When & Then
        MvcResult result = mockMvc.perform(post("/api/auth/setup-mfa")
                        .header("Authorization", jwtToken))
                .andExpect(status().isOk())
                .andReturn();

        // Vérifier la réponse
        Map<String, String> response = objectMapper.readValue(result.getResponse().getContentAsString(), HashMap.class);
        assertNotNull(response.get("secret"));
        assertNotNull(response.get("qrCodeUrl"));
        assertTrue(response.get("qrCodeUrl").toString().startsWith("otpauth://totp/MosaiqueCapital:existinguser"));
    }
}