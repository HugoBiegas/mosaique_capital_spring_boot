// com/master/mosaique_capital/service/banking/BankingSyncSchedulerService.java
package com.master.mosaique_capital.service.banking;

import com.master.mosaique_capital.dto.banking.BankSyncResponse;
import com.master.mosaique_capital.entity.BankConnection;
import com.master.mosaique_capital.entity.User;
import com.master.mosaique_capital.repository.BankConnectionRepository;
import com.master.mosaique_capital.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service de synchronisation automatique des connexions bancaires
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(value = "app.banking.sync.enabled", havingValue = "true", matchIfMissing = true)
public class BankingSyncSchedulerService {

    private final BankConnectionRepository bankConnectionRepository;
    private final UserRepository userRepository;
    private final BankAccountSyncService bankAccountSyncService;
    private final BankingNotificationService bankingNotificationService;

    @Value("${app.banking.sync.interval:PT6H}")
    private String syncInterval;

    @Value("${app.banking.sync.max-connections-per-batch:5}")
    private int maxConnectionsPerBatch;

    @Value("${app.banking.sync.stale-hours:6}")
    private int staleHours;


    @Scheduled(cron = "0 0 */6 * * *")
    @Transactional
    public void scheduledSyncAll() {
        log.info("🔄 Début de la synchronisation automatique programmée");

        try {
            LocalDateTime threshold = LocalDateTime.now().minusHours(staleHours);
            List<BankConnection> staleConnections = bankConnectionRepository.findConnectionsNeedingSync(threshold);

            if (staleConnections.isEmpty()) {
                log.info("✅ Aucune connexion nécessitant une synchronisation");
                return;
            }

            log.info("📊 {} connexions nécessitent une synchronisation", staleConnections.size());

            // Traitement par batch pour éviter la surcharge
            for (int i = 0; i < staleConnections.size(); i += maxConnectionsPerBatch) {
                int endIndex = Math.min(i + maxConnectionsPerBatch, staleConnections.size());
                List<BankConnection> batch = staleConnections.subList(i, endIndex);

                log.info("🔄 Traitement du batch {}-{}/{}", i + 1, endIndex, staleConnections.size());
                processSyncBatch(batch);

                // Pause entre les batches pour éviter le rate limiting
                if (endIndex < staleConnections.size()) {
                    try {
                        Thread.sleep(2000); // 2 secondes entre les batches
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.warn("Interruption détectée pendant la pause");
                        break;
                    }
                }
            }

            log.info("✅ Synchronisation automatique terminée avec succès");

        } catch (Exception e) {
            log.error("❌ Erreur lors de la synchronisation automatique: {}", e.getMessage(), e);
        }
    }

    /**
     * Synchronisation asynchrone d'un batch de connexions
     */
    @Async("bankingTaskExecutor")
    public CompletableFuture<Void> processSyncBatch(List<BankConnection> connections) {
        log.info("🔄 Début de la synchronisation de {} connexions", connections.size());

        int successCount = 0;
        int errorCount = 0;

        for (BankConnection connection : connections) {
            try {
                BankSyncResponse response = bankAccountSyncService.syncAccountsForConnection(connection);

                if (response.isSuccess()) {
                    successCount++;
                    log.debug("✅ Connexion {} synchronisée: {} comptes, {} transactions",
                            connection.getId(), response.getAccountsSynced(), response.getTransactionsSynced());

                    // Notification si nouvelles transactions importantes
                    if (response.getStatistics() != null && response.getStatistics().getNewTransactions() > 10) {
                        bankingNotificationService.notifyNewTransactions(
                                connection.getUser(),
                                connection,
                                response.getStatistics().getNewTransactions()
                        );
                    }
                } else {
                    errorCount++;
                    log.warn("⚠️ Échec de synchronisation pour la connexion {}: {}",
                            connection.getId(), response.getMessage());

                    // Marquer la connexion comme en erreur si échec répété
                    if (isRepeatedFailure(connection)) {
                        connection.setConnectionStatus("ERROR");
                        bankConnectionRepository.save(connection);

                        bankingNotificationService.notifyConnectionError(connection.getUser(), connection);
                    }
                }

            } catch (Exception e) {
                errorCount++;
                log.error("❌ Erreur lors de la synchronisation de la connexion {}: {}",
                        connection.getId(), e.getMessage(), e);
            }
        }

        log.info("📊 Batch terminé: {} succès, {} erreurs", successCount, errorCount);
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Synchronisation manuelle d'un utilisateur spécifique
     */
    @Async("bankingTaskExecutor")
    @Transactional
    public CompletableFuture<List<BankSyncResponse>> syncUserConnections(Long userId) {
        log.info("🔄 Synchronisation manuelle pour l'utilisateur ID: {}", userId);

        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé: " + userId));

            List<BankConnection> activeConnections = bankConnectionRepository.findByUserAndConnectionStatus(user, "ACTIVE");

            if (activeConnections.isEmpty()) {
                log.info("ℹ️ Aucune connexion active pour l'utilisateur {}", userId);
                return CompletableFuture.completedFuture(List.of());
            }

            List<BankSyncResponse> responses = activeConnections.stream()
                    .map(connection -> {
                        try {
                            return bankAccountSyncService.syncAccountsForConnection(connection);
                        } catch (Exception e) {
                            log.error("Erreur sync connexion {}: {}", connection.getId(), e.getMessage());
                            return BankSyncResponse.error(connection.getId(), e.getMessage());
                        }
                    })
                    .toList();

            log.info("✅ Synchronisation manuelle terminée pour l'utilisateur {}: {} connexions traitées",
                    userId, responses.size());

            return CompletableFuture.completedFuture(responses);

        } catch (Exception e) {
            log.error("❌ Erreur lors de la synchronisation manuelle utilisateur {}: {}", userId, e.getMessage(), e);
            return CompletableFuture.completedFuture(List.of());
        }
    }

    /**
     * Nettoyage des connexions inactives (tous les jours à 2h)
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupInactiveConnections() {
        log.info("🧹 Début du nettoyage des connexions inactives");

        try {
            LocalDateTime threshold = LocalDateTime.now().minusDays(30);
            List<BankConnection> inactiveConnections = bankConnectionRepository.findConnectionsNeedingSync(threshold);

            int cleanedCount = 0;
            for (BankConnection connection : inactiveConnections) {
                if ("ERROR".equals(connection.getConnectionStatus()) ||
                        "EXPIRED".equals(connection.getConnectionStatus())) {

                    log.info("🗑️ Nettoyage de la connexion inactive: {}", connection.getId());

                    // Notification à l'utilisateur avant suppression
                    bankingNotificationService.notifyConnectionCleanup(connection.getUser(), connection);

                    // Marquer comme inactive plutôt que supprimer
                    connection.setConnectionStatus("INACTIVE");
                    bankConnectionRepository.save(connection);
                    cleanedCount++;
                }
            }

            log.info("✅ Nettoyage terminé: {} connexions marquées comme inactives", cleanedCount);

        } catch (Exception e) {
            log.error("❌ Erreur lors du nettoyage: {}", e.getMessage(), e);
        }
    }

    /**
     * Rapport de santé des connexions (tous les lundis à 9h)
     */
    @Scheduled(cron = "0 0 9 * * MON")
    @Transactional(readOnly = true)
    public void generateHealthReport() {
        log.info("📊 Génération du rapport de santé des connexions bancaires");

        try {
            List<User> allUsers = userRepository.findAll();

            int totalUsers = allUsers.size();
            int usersWithConnections = 0;
            int totalActiveConnections = 0;
            int totalErrorConnections = 0;

            for (User user : allUsers) {
                List<BankConnection> userConnections = bankConnectionRepository.findByUserOrderByCreatedAtDesc(user);

                if (!userConnections.isEmpty()) {
                    usersWithConnections++;

                    long activeCount = userConnections.stream()
                            .mapToLong(conn -> "ACTIVE".equals(conn.getConnectionStatus()) ? 1 : 0)
                            .sum();

                    long errorCount = userConnections.stream()
                            .mapToLong(conn -> "ERROR".equals(conn.getConnectionStatus()) ? 1 : 0)
                            .sum();

                    totalActiveConnections += activeCount;
                    totalErrorConnections += errorCount;
                }
            }

            log.info("📊 RAPPORT DE SANTÉ BANCAIRE - {}",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            log.info("👥 Utilisateurs total: {}", totalUsers);
            log.info("🔗 Utilisateurs avec connexions: {}", usersWithConnections);
            log.info("✅ Connexions actives: {}", totalActiveConnections);
            log.info("❌ Connexions en erreur: {}", totalErrorConnections);
            log.info("📈 Taux d'adoption: {:.1f}%",
                    totalUsers > 0 ? (usersWithConnections * 100.0 / totalUsers) : 0);

        } catch (Exception e) {
            log.error("❌ Erreur lors de la génération du rapport: {}", e.getMessage(), e);
        }
    }

    /**
     * Vérifie si une connexion a des échecs répétés
     */
    private boolean isRepeatedFailure(BankConnection connection) {
        // Simple heuristique: si pas de sync réussie depuis plus de 24h
        return connection.getLastSyncAt() == null ||
                connection.getLastSyncAt().isBefore(LocalDateTime.now().minusHours(24));
    }
}