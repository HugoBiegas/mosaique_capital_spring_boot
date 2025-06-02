// com/master/mosaique_capital/controller/MfaController.java
package com.master.mosaique_capital.controller;

import com.master.mosaique_capital.dto.mfa.*;
import com.master.mosaique_capital.service.mfa.MfaService;
import com.master.mosaique_capital.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/mfa")
@RequiredArgsConstructor
@Slf4j
public class MfaController {

    private final MfaService mfaService;

    @PostMapping("/setup")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Map<String, Object>> setupMfa() {
        String username = SecurityUtils.getCurrentUsername();
        log.info("Demande de configuration MFA pour l'utilisateur: {}", username);

        MfaSetupResponse mfaSetup = mfaService.generateMfaSetup(username);

        Map<String, Object> response = new HashMap<>();
        response.put("secret", mfaSetup.getSecret());
        response.put("qrCodeUrl", mfaSetup.getQrCodeUrl());
        response.put("message", "Configuration MFA générée. Scannez le QR code avec votre application d'authentification.");
        response.put("instructions", "Utilisez Google Authenticator, Authy ou une application similaire pour scanner le QR code.");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/qrcode")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<byte[]> getQrCode() {
        String username = SecurityUtils.getCurrentUsername();
        log.info("Demande de QR code MFA pour l'utilisateur: {}", username);

        MfaSetupResponse mfaSetup = mfaService.generateMfaSetup(username);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        headers.setContentDispositionFormData("attachment", "mfa-qrcode.png");

        return ResponseEntity.ok()
                .headers(headers)
                .body(mfaSetup.getQrCodeImage());
    }

    @PostMapping("/verify")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Map<String, String>> verifyMfa(@Valid @RequestBody MfaVerificationRequest request) {
        String username = SecurityUtils.getCurrentUsername();
        log.info("Tentative de vérification MFA pour l'utilisateur: {}", username);

        mfaService.verifyAndEnableMfa(username, request.getCode());

        Map<String, String> response = new HashMap<>();
        response.put("message", "Authentification à deux facteurs activée avec succès");
        response.put("status", "enabled");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/disable")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Map<String, String>> disableMfa(@Valid @RequestBody MfaDisableRequest request) {
        String username = SecurityUtils.getCurrentUsername();
        log.info("Tentative de désactivation MFA pour l'utilisateur: {}", username);

        mfaService.disableMfa(username, request.getCurrentMfaCode());

        Map<String, String> response = new HashMap<>();
        response.put("message", "Authentification à deux facteurs désactivée avec succès");
        response.put("status", "disabled");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/regenerate")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Map<String, Object>> regenerateMfa(@Valid @RequestBody MfaDisableRequest request) {
        String username = SecurityUtils.getCurrentUsername();
        log.info("Tentative de régénération MFA pour l'utilisateur: {}", username);

        MfaSetupResponse mfaSetup = mfaService.regenerateMfaSecret(username, request.getCurrentMfaCode());

        Map<String, Object> response = new HashMap<>();
        response.put("secret", mfaSetup.getSecret());
        response.put("qrCodeUrl", mfaSetup.getQrCodeUrl());
        response.put("message", "Nouveau secret MFA généré. Reconfigurez votre application d'authentification.");
        response.put("warning", "L'ancien secret est maintenant invalide. Vous devez reconfigurer votre application.");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<MfaStatusResponse> getMfaStatus() {
        String username = SecurityUtils.getCurrentUsername();
        boolean isEnabled = mfaService.isMfaEnabled(username);

        String message = isEnabled
                ? "L'authentification à deux facteurs est activée"
                : "L'authentification à deux facteurs est désactivée";

        return ResponseEntity.ok(new MfaStatusResponse(isEnabled, message));
    }

    @PostMapping("/validate")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Map<String, Object>> validateMfaCode(@Valid @RequestBody MfaVerificationRequest request) {
        String username = SecurityUtils.getCurrentUsername();
        boolean isValid = mfaService.verifyMfaCode(username, request.getCode());

        Map<String, Object> response = new HashMap<>();
        response.put("valid", isValid);
        response.put("message", isValid ? "Code MFA valide" : "Code MFA invalide");

        HttpStatus status = isValid ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(response);
    }
}