// com/master/mosaique_capital/controller/AuthController.java
package com.master.mosaique_capital.controller;

import com.master.mosaique_capital.dto.auth.JwtResponse;
import com.master.mosaique_capital.dto.auth.LoginRequest;
import com.master.mosaique_capital.dto.auth.SignupRequest;
import com.master.mosaique_capital.service.auth.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<Map<String, String>> registerUser(@Valid @RequestBody SignupRequest signupRequest) {
        log.info("Tentative de création de compte pour l'utilisateur: {}", signupRequest.getUsername());

        authService.registerUser(signupRequest);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Utilisateur enregistré avec succès");
        response.put("username", signupRequest.getUsername());
        response.put("status", "created");

        log.info("Compte créé avec succès pour l'utilisateur: {}", signupRequest.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<JwtResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("Tentative de connexion pour l'utilisateur: {}", loginRequest.getUsername());

        JwtResponse jwtResponse = authService.authenticateUser(loginRequest);

        log.info("Connexion réussie pour l'utilisateur: {}", loginRequest.getUsername());
        return ResponseEntity.ok(jwtResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refreshToken(@RequestBody Map<String, String> refreshRequest) {
        String refreshToken = refreshRequest.get("refreshToken");

        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Refresh token manquant");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        log.info("Tentative de rafraîchissement de token");

        String newAccessToken = authService.refreshAccessToken(refreshToken);

        Map<String, String> response = new HashMap<>();
        response.put("accessToken", newAccessToken);
        response.put("tokenType", "Bearer");
        response.put("message", "Token rafraîchi avec succès");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@RequestHeader("Authorization") String authHeader) {
        log.info("Tentative de déconnexion");

        // Extraction du token de l'en-tête Authorization
        String token = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }

        authService.logout(token);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Déconnexion réussie");
        response.put("status", "logged_out");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        String token = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }

        Map<String, Object> userInfo = authService.getCurrentUserInfo(token);
        return ResponseEntity.ok(userInfo);
    }
}