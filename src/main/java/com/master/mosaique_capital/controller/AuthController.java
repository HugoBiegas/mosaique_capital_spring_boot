// com/master/mosaique_capital/controller/AuthController.java
package com.master.mosaique_capital.controller;

import com.master.mosaique_capital.dto.auth.JwtResponse;
import com.master.mosaique_capital.dto.auth.LoginRequest;
import com.master.mosaique_capital.dto.auth.SignupRequest;
import com.master.mosaique_capital.service.auth.AuthService;
import com.master.mosaique_capital.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signupRequest) {
        authService.registerUser(signupRequest);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Utilisateur enregistré avec succès");

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<JwtResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        JwtResponse jwtResponse = authService.authenticateUser(loginRequest);
        return ResponseEntity.ok(jwtResponse);
    }

    @PostMapping("/setup-mfa")
    public ResponseEntity<?> setupMfa() {
        String username = SecurityUtils.getCurrentUsername();
        String secret = authService.generateMfaSecret(username);

        // Génération de l'URL pour le QR code
        String qrCodeUrl = generateQrCodeUrl(username, secret);

        Map<String, String> response = new HashMap<>();
        response.put("secret", secret);
        response.put("qrCodeUrl", qrCodeUrl);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-mfa")
    public ResponseEntity<?> verifyMfa(@RequestParam String code) {
        String username = SecurityUtils.getCurrentUsername();
        authService.enableMfa(username, code);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Authentification à deux facteurs activée avec succès");

        return ResponseEntity.ok(response);
    }

    private String generateQrCodeUrl(String username, String secret) {
        return String.format("otpauth://totp/MosaiqueCapital:%s?secret=%s&issuer=MosaiqueCapital",
                username, secret);
    }
}