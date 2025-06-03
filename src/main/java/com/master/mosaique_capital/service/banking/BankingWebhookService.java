// com/master/mosaique_capital/service/banking/BankingWebhookService.java
package com.master.mosaique_capital.service.banking;

import com.master.mosaique_capital.entity.BankAccount;
import com.master.mosaique_capital.entity.BankConnection;
import com.master.mosaique_capital.entity.User;
import com.master.mosaique_capital.repository.BankAccountRepository;
import com.master.mosaique_capital.repository.BankConnectionRepository;
import com.master.mosaique_capital.repository.UserRepository;
import com.master.mosaique_capital.service.banking.external.BankAggregationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;

/**
 * Service de gestion des webhooks bancaires
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BankingWebhookService {

    private final BankConnectionRepository bankConnectionRepository;
    private final BankAccountRepository bankAccountRepository;
    private final UserRepository userRepository;
    private final BankAccountSyncService bankAccountSyncService;
    private final BankingNotificationService notificationService;

    @Value("${app.banking.budget-insight.webhook-secret}")
    private String budgetInsightWebhookSecret;

    @Value("${app.banking.linxo.webhook-secret}")
    private String linxoWebhookSecret;

    /**
     * Traite un webhook Budget Insight
     */
    @Async("bankingTaskExecutor")
    @Transactional
    public boolean processBudgetInsightWebhook(Map<String, Object> payload) {
        try {
            String eventType = (String) payload.get("type");
            String connectionId = extractConnectionId(payload);

            log.info("🔄 Traitement webhook Budget Insight - Type: {}, Connection: {}", eventType, connectionId);

            return switch (eventType) {
                case "connection.synced" -> handleConnectionSynced(connectionId);
                case "connection.error" -> handleConnectionError(connectionId, payload);
                case "connection.expired" -> handleConnectionExpired(connectionId);
                case "account.updated" -> handleAccountUpdated(payload);
                case "transaction.created" -> handleTransactionCreated(payload);
                default -> {
                    log.warn("⚠️ Type de webhook Budget Insight non géré: {}", eventType);
                    yield false;
                }
            };

        } catch (Exception e) {
            log.error("❌ Erreur lors du traitement du webhook Budget Insight: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Traite un webhook Linxo
     */
    @Async("bankingTaskExecutor")
    @Transactional
    public boolean processLinxoWebhook(Map<String, Object> payload) {
        try {
            String eventType = (String) payload.get("event");
            log.info("🔄 Traitement webhook Linxo - Event: {}", eventType);

            // TODO: Implémenter le traitement des événements Linxo
            log.info("ℹ️ Traitement webhook Linxo non encore implémenté pour: {}", eventType);
            return true;

        } catch (Exception e) {
            log.error("❌ Erreur lors du traitement du webhook Linxo: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Vérifie la signature d'un webhook Budget Insight
     */
    public boolean verifyBudgetInsightSignature(Map<String, Object> payload, String signature, HttpServletRequest request) {
        if (signature == null || budgetInsightWebhookSecret == null) {
            log.warn("⚠️ Signature ou secret manquant pour Budget Insight");
            return false;
        }

        try {
            String expectedSignature = calculateHmacSha256(payload.toString(), budgetInsightWebhookSecret);
            return signature.equals(expectedSignature);

        } catch (Exception e) {
            log.error("❌ Erreur lors de la vérification de signature Budget Insight: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Vérifie la signature d'un webhook Linxo
     */
    public boolean verifyLinxoSignature(Map<String, Object> payload, String signature, HttpServletRequest request) {
        if (signature == null || linxoWebhookSecret == null) {
            log.warn("⚠️ Signature ou secret manquant pour Linxo");
            return false;
        }

        try {
            String expectedSignature = calculateHmacSha256(payload.toString(), linxoWebhookSecret);
            return signature.equals(expectedSignature);

        } catch (Exception e) {
            log.error("❌ Erreur lors de la vérification de signature Linxo: {}", e.getMessage(), e);
            return false;
        }
    }

    // ===== Handlers spécifiques aux événements =====

    private boolean handleConnectionSynced(String connectionId) {
        Optional<BankConnection> connectionOpt = findConnectionByExternalId(connectionId);

        if (connectionOpt.isEmpty()) {
            log.warn("⚠️ Connexion non trouvée pour l'ID: {}", connectionId);
            return false;
        }

        BankConnection connection = connectionOpt.get();

        try {
            // Déclencher une synchronisation manuelle
            bankAccountSyncService.syncAccountsForConnection(connection);

            log.info("✅ Synchronisation webhook réussie pour la connexion: {}", connectionId);
            return true;

        } catch (Exception e) {
            log.error("❌ Erreur lors de la synchronisation webhook: {}", e.getMessage(), e);
            return false;
        }
    }

    private boolean handleConnectionError(String connectionId, Map<String, Object> payload) {
        Optional<BankConnection> connectionOpt = findConnectionByExternalId(connectionId);

        if (connectionOpt.isEmpty()) {
            log.warn("⚠️ Connexion non trouvée pour l'ID: {}", connectionId);
            return false;
        }

        BankConnection connection = connectionOpt.get();
        String errorMessage = (String) payload.getOrDefault("error", "Erreur inconnue");

        // Mettre à jour le statut de la connexion
        connection.setConnectionStatus("ERROR");
        bankConnectionRepository.save(connection);

        // Notifier l'utilisateur
        notificationService.notifyConnectionError(connection.getUser(), connection);

        log.warn("⚠️ Connexion {} marquée en erreur: {}", connectionId, errorMessage);
        return true;
    }

    private boolean handleConnectionExpired(String connectionId) {
        Optional<BankConnection> connectionOpt = findConnectionByExternalId(connectionId);

        if (connectionOpt.isEmpty()) {
            log.warn("⚠️ Connexion non trouvée pour l'ID: {}", connectionId);
            return false;
        }

        BankConnection connection = connectionOpt.get();

        // Mettre à jour le statut
        connection.setConnectionStatus("EXPIRED");
        bankConnectionRepository.save(connection);

        // Notifier l'utilisateur pour renouveler la connexion
        notificationService.notifyConnectionError(connection.getUser(), connection);

        log.info("⏰ Connexion {} expirée", connectionId);
        return true;
    }

    private boolean handleAccountUpdated(Map<String, Object> payload) {
        try {
            String accountId = (String) payload.get("account_id");
            String connectionId = extractConnectionId(payload);

            Optional<BankConnection> connectionOpt = findConnectionByExternalId(connectionId);
            if (connectionOpt.isEmpty()) {
                return false;
            }

            Optional<BankAccount> accountOpt = bankAccountRepository
                    .findByConnectionAndAccountId(connectionOpt.get(), accountId);

            if (accountOpt.isPresent()) {
                // Déclencher une resynchronisation du compte spécifique
                bankAccountSyncService.syncAccountsForConnection(connectionOpt.get());
                log.info("✅ Compte {} mis à jour via webhook", accountId);
                return true;
            }

            return false;

        } catch (Exception e) {
            log.error("❌ Erreur lors de la mise à jour du compte: {}", e.getMessage(), e);
            return false;
        }
    }

    private boolean handleTransactionCreated(Map<String, Object> payload) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> transactionData = (Map<String, Object>) payload.get("transaction");

            String connectionId = extractConnectionId(payload);
            String accountId = (String) transactionData.get("account_id");

            Optional<BankConnection> connectionOpt = findConnectionByExternalId(connectionId);
            if (connectionOpt.isEmpty()) {
                return false;
            }

            // Déclencher une synchronisation pour récupérer les nouvelles transactions
            bankAccountSyncService.syncAccountsForConnection(connectionOpt.get());

            log.info("✅ Nouvelle transaction détectée et synchronisée pour le compte: {}", accountId);
            return true;

        } catch (Exception e) {
            log.error("❌ Erreur lors du traitement de la nouvelle transaction: {}", e.getMessage(), e);
            return false;
        }
    }

    // ===== Méthodes utilitaires =====

    private String extractConnectionId(Map<String, Object> payload) {
        // Logic pour extraire l'ID de connexion selon le format du provider
        return (String) payload.get("connection_id");
    }

    private Optional<BankConnection> findConnectionByExternalId(String externalId) {
        // Recherche par ID externe (avec préfixes)
        String searchId = externalId.startsWith("bi_") ? externalId : "bi_" + externalId;

        return bankConnectionRepository.findAll().stream()
                .filter(conn -> searchId.equals(conn.getConnectionId()))
                .findFirst();
    }

    private String calculateHmacSha256(String data, String secret) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);

        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }
}