// com/master/mosaique_capital/exception/BankingGlobalExceptionHandler.java
package com.master.mosaique_capital.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Gestionnaire d'exceptions spécifique aux APIs bancaires
 */
@ControllerAdvice(basePackages = "com.master.mosaique_capital.controller")
@Slf4j
public class BankingGlobalExceptionHandler {

    @ExceptionHandler(BankConnectionException.class)
    public ResponseEntity<Map<String, Object>> handleBankConnectionException(BankConnectionException ex) {
        log.error("❌ Erreur de connexion bancaire: {}", ex.getMessage(), ex);

        Map<String, Object> errorResponse = Map.of(
                "error", "BANK_CONNECTION_ERROR",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now(),
                "suggestion", "Vérifiez vos identifiants bancaires et réessayez"
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(BankSyncException.class)
    public ResponseEntity<Map<String, Object>> handleBankSyncException(BankSyncException ex) {
        log.error("❌ Erreur de synchronisation bancaire: {}", ex.getMessage(), ex);

        Map<String, Object> errorResponse = Map.of(
                "error", "BANK_SYNC_ERROR",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now(),
                "suggestion", "La synchronisation sera réessayée automatiquement"
        );

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
    }

    @ExceptionHandler(WebhookVerificationException.class)
    public ResponseEntity<Map<String, Object>> handleWebhookVerificationException(WebhookVerificationException ex) {
        log.error("❌ Erreur de vérification webhook: {}", ex.getMessage(), ex);

        Map<String, Object> errorResponse = Map.of(
                "error", "WEBHOOK_VERIFICATION_ERROR",
                "message", "Signature de webhook invalide",
                "timestamp", LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }
}
