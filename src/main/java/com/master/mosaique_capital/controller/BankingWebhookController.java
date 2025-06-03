// com/master/mosaique_capital/controller/BankingWebhookController.java
package com.master.mosaique_capital.controller;

import com.master.mosaique_capital.service.banking.BankingWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Contrôleur pour la réception des webhooks bancaires
 */
@RestController
@RequestMapping("/api/banking/webhooks")
@RequiredArgsConstructor
@Slf4j
public class BankingWebhookController {

    private final BankingWebhookService webhookService;

    /**
     * Webhook Budget Insight
     * Documentation: https://docs.budget-insight.com/guides/webhooks
     */
    @PostMapping("/budget-insight")
    public ResponseEntity<Map<String, String>> handleBudgetInsightWebhook(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "X-Webhook-Signature", required = false) String signature,
            HttpServletRequest request) {

        log.info("📨 Réception webhook Budget Insight: {}", payload.get("type"));

        try {
            // Vérification de la signature pour la sécurité
            if (!webhookService.verifyBudgetInsightSignature(payload, signature, request)) {
                log.warn("⚠️ Signature webhook Budget Insight invalide");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid signature"));
            }

            // Traitement du webhook
            boolean processed = webhookService.processBudgetInsightWebhook(payload);

            if (processed) {
                return ResponseEntity.ok(Map.of("status", "processed"));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Failed to process webhook"));
            }

        } catch (Exception e) {
            log.error("❌ Erreur lors du traitement du webhook Budget Insight: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error"));
        }
    }

    /**
     * Webhook Linxo
     */
    @PostMapping("/linxo")
    public ResponseEntity<Map<String, String>> handleLinxoWebhook(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "X-Linxo-Signature", required = false) String signature,
            HttpServletRequest request) {

        log.info("📨 Réception webhook Linxo: {}", payload.get("event"));

        try {
            if (!webhookService.verifyLinxoSignature(payload, signature, request)) {
                log.warn("⚠️ Signature webhook Linxo invalide");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid signature"));
            }

            boolean processed = webhookService.processLinxoWebhook(payload);

            return processed ?
                    ResponseEntity.ok(Map.of("status", "processed")) :
                    ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Failed to process webhook"));

        } catch (Exception e) {
            log.error("❌ Erreur lors du traitement du webhook Linxo: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error"));
        }
    }

    /**
     * Endpoint de test pour vérifier la connectivité des webhooks
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> webhookHealth() {
        return ResponseEntity.ok(Map.of(
                "status", "healthy",
                "timestamp", java.time.LocalDateTime.now(),
                "version", "1.0.0"
        ));
    }
}
