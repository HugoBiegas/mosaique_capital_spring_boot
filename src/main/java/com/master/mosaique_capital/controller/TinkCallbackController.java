// src/main/java/com/master/mosaique_capital/controller/TinkCallbackController.java
package com.master.mosaique_capital.controller;

import com.master.mosaique_capital.service.banking.BankConnectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Map;

/**
 * Contrôleur pour gérer les callbacks de retour Tink
 * Après authentification utilisateur chez la banque
 */
@RestController
@RequestMapping("/api/banking/webhooks/tink")
@RequiredArgsConstructor
@Slf4j
public class TinkCallbackController {

    private final BankConnectionService bankConnectionService;

    /**
     * 🔄 Callback après authentification bancaire réussie chez Tink
     * L'utilisateur est redirigé ici après avoir autorisé l'accès à ses comptes
     */
    @GetMapping("/callback")
    public RedirectView handleTinkCallback(
            @RequestParam(value = "ref", required = false) String reference,
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "details", required = false) String errorDetails) {

        log.info("📥 Callback Tink reçu - Ref: {}, Error: {}", reference, error);

        try {
            if (error != null && !error.trim().isEmpty()) {
                // Erreur lors de l'authentification
                log.warn("❌ Erreur callback Tink: {} - {}", error, errorDetails);

                // Redirection vers frontend avec erreur
                return new RedirectView(
                        String.format("http://localhost:3000/banking/connection-error?error=%s&details=%s",
                                error, errorDetails != null ? errorDetails : "")
                );
            }

            if (reference != null && reference.startsWith("mosaique_")) {
                // Succès - L'utilisateur a autorisé l'accès
                log.info("✅ Callback Tink réussi pour référence: {}", reference);

                // Redirection vers frontend avec succès
                return new RedirectView(
                        String.format("http://localhost:3000/banking/connection-success?ref=%s", reference)
                );
            }

            // Cas par défaut
            log.warn("⚠️ Callback Tink sans paramètres valides");
            return new RedirectView("http://localhost:3000/banking/connection-pending");

        } catch (Exception e) {
            log.error("❌ Erreur lors du traitement du callback Tink: {}", e.getMessage(), e);
            return new RedirectView("http://localhost:3000/banking/connection-error?error=callback_error");
        }
    }

    /**
     * 🔍 Endpoint pour vérifier le statut d'une connexion Tink
     * Appelé par le frontend pour connaître l'état de la connexion
     */
    @GetMapping("/status/{connectionId}")
    public ResponseEntity<Map<String, Object>> checkConnectionStatus(@PathVariable String connectionId) {
        log.info("🔍 Vérification statut connexion Tink: {}", connectionId);

        try {
            // Vérifier si la connexion est active
            boolean isHealthy = bankConnectionService.isConnectionHealthy(Long.parseLong(connectionId));

            Map<String, Object> status = Map.of(
                    "connectionId", connectionId,
                    "status", isHealthy ? "ACTIVE" : "PENDING",
                    "healthy", isHealthy,
                    "provider", "tink",
                    "message", isHealthy ? "Connexion Tink active" : "Connexion Tink en attente",
                    "timestamp", java.time.LocalDateTime.now()
            );

            return ResponseEntity.ok(status);

        } catch (Exception e) {
            log.error("❌ Erreur lors de la vérification du statut Tink: {}", e.getMessage(), e);

            Map<String, Object> errorStatus = Map.of(
                    "connectionId", connectionId,
                    "status", "ERROR",
                    "healthy", false,
                    "error", e.getMessage(),
                    "timestamp", java.time.LocalDateTime.now()
            );

            return ResponseEntity.ok(errorStatus);
        }
    }

    /**
     * 📋 Endpoint pour obtenir l'URL d'autorisation Tink
     * Utilisé par le frontend pour rediriger l'utilisateur vers sa banque
     */
    @PostMapping("/authorization-url")
    public ResponseEntity<Map<String, String>> getAuthorizationUrl(
            @RequestBody Map<String, String> request) {

        String requisitionId = request.get("requisitionId");
        String institutionId = request.get("institutionId");

        log.info("🔗 Génération URL d'autorisation Tink pour requisition: {}", requisitionId);

        try {
            // URL d'autorisation Tink (format standard)
            String authUrl = String.format(
                    "https://ob.nordigen.com/psd2/start/%s/%s",
                    requisitionId,
                    institutionId
            );

            Map<String, String> response = Map.of(
                    "authorizationUrl", authUrl,
                    "requisitionId", requisitionId,
                    "provider", "tink",
                    "message", "Redirecting to bank authentication...",
                    "expiresIn", "300" // 5 minutes
            );

            log.info("✅ URL d'autorisation Tink générée: {}", authUrl);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Erreur lors de la génération de l'URL Tink: {}", e.getMessage(), e);

            Map<String, String> errorResponse = Map.of(
                    "error", "URL_GENERATION_ERROR",
                    "message", "Impossible de générer l'URL d'autorisation Tink",
                    "details", e.getMessage()
            );

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 🎯 Endpoint de test pour valider l'intégration Tink
     */
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> testTinkIntegration() {
        log.info("🧪 Test d'intégration Tink GRATUITE");

        Map<String, Object> testResult = Map.of(
                "provider", "tink",
                "status", "AVAILABLE",
                "pricing", "FREE - 100 connections/month",
                "features", Map.of(
                        "accounts", true,
                        "transactions", true,
                        "balances", true,
                        "psd2_native", true
                ),
                "supported_countries", java.util.List.of("FR", "DE", "ES", "IT", "NL", "BE", "AT"),
                "institutions_count", "2500+",
                "message", "🎉 Tink est opérationnel et GRATUIT pour vos démos !",
                "documentation", "https://docs.tink.com/api",
                "timestamp", java.time.LocalDateTime.now()
        );

        return ResponseEntity.ok(testResult);
    }
}