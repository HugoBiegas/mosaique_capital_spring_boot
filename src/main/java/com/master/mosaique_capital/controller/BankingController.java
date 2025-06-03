// com/master/mosaique_capital/controller/BankingController.java
package com.master.mosaique_capital.controller;

import com.master.mosaique_capital.dto.banking.*;
import com.master.mosaique_capital.service.banking.BankConnectionService;
import com.master.mosaique_capital.service.banking.BankAccountService;
import com.master.mosaique_capital.service.banking.BankTransactionService;
import com.master.mosaique_capital.service.banking.external.BankAggregationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Contrôleur REST pour la gestion des connexions bancaires et agrégation de données
 */
@RestController
@RequestMapping("/api/banking")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ROLE_USER')")
public class BankingController {

    private final BankConnectionService bankConnectionService;
    private final BankAccountService bankAccountService;
    private final BankTransactionService bankTransactionService;
    private final BankAggregationService bankAggregationService;

    // ===== GESTION DES CONNEXIONS BANCAIRES =====

    /**
     * Récupère toutes les connexions bancaires de l'utilisateur
     */
    @GetMapping("/connections")
    public ResponseEntity<List<BankConnectionDto>> getAllConnections() {
        log.info("Récupération de toutes les connexions bancaires");
        return ResponseEntity.ok(bankConnectionService.getAllConnections());
    }

    /**
     * Récupère une connexion bancaire par ID
     */
    @GetMapping("/connections/{id}")
    public ResponseEntity<BankConnectionDto> getConnection(@PathVariable Long id) {
        log.info("Récupération de la connexion bancaire ID: {}", id);
        return ResponseEntity.ok(bankConnectionService.getConnectionById(id));
    }

    /**
     * Récupère la liste des providers bancaires disponibles
     */
    @GetMapping("/providers")
    public ResponseEntity<List<BankProviderDto>> getAvailableProviders() {
        return ResponseEntity.ok(bankAggregationService.getAvailableProviders());
    }

    /**
     * Initie une nouvelle connexion bancaire
     */
    @PostMapping("/connections")
    public ResponseEntity<BankConnectionDto> initiateConnection(@Valid @RequestBody BankConnectionRequest request) {
        log.info("Initiation d'une nouvelle connexion bancaire avec le provider: {}", request.getProvider());
        BankConnectionDto connection = bankConnectionService.initiateConnection(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(connection);
    }

    /**
     * Confirme une connexion bancaire après authentification forte
     */
    @PostMapping("/connections/{id}/confirm")
    public ResponseEntity<BankConnectionDto> confirmConnection(
            @PathVariable Long id,
            @RequestBody Map<String, String> confirmationData) {

        String confirmationCode = confirmationData.get("confirmationCode");
        log.info("Confirmation de la connexion bancaire ID: {}", id);

        BankConnectionDto connection = bankConnectionService.confirmConnection(id, confirmationCode);
        return ResponseEntity.ok(connection);
    }

    /**
     * Synchronise une connexion bancaire
     */
    @PostMapping("/connections/{id}/sync")
    public ResponseEntity<BankSyncResponse> synchronizeConnection(@PathVariable Long id) {
        log.info("Synchronisation de la connexion bancaire ID: {}", id);
        BankSyncResponse syncResponse = bankConnectionService.synchronizeConnection(id);
        return ResponseEntity.ok(syncResponse);
    }

    /**
     * Synchronise toutes les connexions actives
     */
    @PostMapping("/connections/sync-all")
    public ResponseEntity<List<BankSyncResponse>> synchronizeAllConnections() {
        log.info("Synchronisation de toutes les connexions bancaires actives");
        List<BankSyncResponse> syncResponses = bankConnectionService.resyncAllActiveConnections();
        return ResponseEntity.ok(syncResponses);
    }

    /**
     * Vérifie l'état de santé d'une connexion
     */
    @GetMapping("/connections/{id}/health")
    public ResponseEntity<Map<String, Object>> checkConnectionHealth(@PathVariable Long id) {
        boolean isHealthy = bankConnectionService.isConnectionHealthy(id);
        Map<String, Object> response = Map.of(
                "connectionId", id,
                "healthy", isHealthy,
                "status", isHealthy ? "OK" : "ERROR",
                "checkedAt", java.time.LocalDateTime.now()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Supprime une connexion bancaire
     */
    @DeleteMapping("/connections/{id}")
    public ResponseEntity<Void> deleteConnection(@PathVariable Long id) {
        log.info("Suppression de la connexion bancaire ID: {}", id);
        bankConnectionService.deleteConnection(id);
        return ResponseEntity.noContent().build();
    }

    // ===== GESTION DES COMPTES BANCAIRES =====

    /**
     * Récupère tous les comptes bancaires de l'utilisateur
     */
    @GetMapping("/accounts")
    public ResponseEntity<List<BankAccountDto>> getAllAccounts() {
        return ResponseEntity.ok(bankAccountService.getAllAccounts());
    }

    /**
     * Récupère un compte bancaire par ID
     */
    @GetMapping("/accounts/{id}")
    public ResponseEntity<BankAccountDto> getAccount(@PathVariable Long id) {
        return ResponseEntity.ok(bankAccountService.getAccountById(id));
    }

    /**
     * Récupère les comptes d'une connexion bancaire spécifique
     */
    @GetMapping("/connections/{connectionId}/accounts")
    public ResponseEntity<List<BankAccountDto>> getAccountsByConnection(@PathVariable Long connectionId) {
        return ResponseEntity.ok(bankAccountService.getAccountsByConnection(connectionId));
    }

    /**
     * Récupère le résumé financier global
     */
    @GetMapping("/summary")
    public ResponseEntity<AccountSummaryDto> getAccountsSummary() {
        return ResponseEntity.ok(bankAccountService.getAccountsSummary());
    }

    // ===== GESTION DES TRANSACTIONS =====

    /**
     * Recherche des transactions avec critères
     */
    @PostMapping("/transactions/search")
    public ResponseEntity<Page<BankTransactionDto>> searchTransactions(
            @RequestBody TransactionSearchCriteria criteria) {

        Page<BankTransactionDto> transactions = bankTransactionService.searchTransactions(criteria);
        return ResponseEntity.ok(transactions);
    }

    /**
     * Récupère les transactions d'un compte
     */
    @GetMapping("/accounts/{accountId}/transactions")
    public ResponseEntity<Page<BankTransactionDto>> getAccountTransactions(
            @PathVariable Long accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "transactionDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        TransactionSearchCriteria criteria = new TransactionSearchCriteria();
        criteria.setAccountId(accountId);
        criteria.setPage(page);
        criteria.setSize(size);
        criteria.setSortBy(sortBy);
        criteria.setSortDirection(sortDir);

        Page<BankTransactionDto> transactions = bankTransactionService.searchTransactions(criteria);
        return ResponseEntity.ok(transactions);
    }

    /**
     * Récupère une transaction par ID
     */
    @GetMapping("/transactions/{id}")
    public ResponseEntity<BankTransactionDto> getTransaction(@PathVariable Long id) {
        return ResponseEntity.ok(bankTransactionService.getTransactionById(id));
    }

    /**
     * Met à jour la catégorie d'une transaction
     */
    @PatchMapping("/transactions/{id}/category")
    public ResponseEntity<BankTransactionDto> updateTransactionCategory(
            @PathVariable Long id,
            @RequestBody Map<String, String> categoryData) {

        String category = categoryData.get("category");
        BankTransactionDto transaction = bankTransactionService.updateTransactionCategory(id, category);
        return ResponseEntity.ok(transaction);
    }

    /**
     * Récupère les statistiques de dépenses par catégorie
     */
    @GetMapping("/transactions/statistics/categories")
    public ResponseEntity<Map<String, Object>> getCategoryStatistics(
            @RequestParam(required = false) java.time.LocalDate startDate,
            @RequestParam(required = false) java.time.LocalDate endDate) {

        Map<String, Object> statistics = bankTransactionService.getCategoryStatistics(startDate, endDate);
        return ResponseEntity.ok(statistics);
    }

    /**
     * Récupère les statistiques de revenus vs dépenses
     */
    @GetMapping("/transactions/statistics/cash-flow")
    public ResponseEntity<Map<String, Object>> getCashFlowStatistics(
            @RequestParam(required = false) java.time.LocalDate startDate,
            @RequestParam(required = false) java.time.LocalDate endDate) {

        Map<String, Object> statistics = bankTransactionService.getCashFlowStatistics(startDate, endDate);
        return ResponseEntity.ok(statistics);
    }

    // ===== ENDPOINTS DE MONITORING =====

    /**
     * Récupère le statut global des connexions bancaires
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getBankingStatus() {
        Map<String, Object> status = Map.of(
                "totalConnections", bankConnectionService.getAllConnections().size(),
                "activeConnections", bankConnectionService.getAllConnections().stream()
                        .mapToLong(conn -> "ACTIVE".equals(conn.getConnectionStatus()) ? 1 : 0).sum(),
                "totalAccounts", bankAccountService.getAllAccounts().size(),
                "lastSyncAt", Objects.requireNonNull(bankConnectionService.getAllConnections().stream()
                        .map(BankConnectionDto::getLastSyncAt)
                        .filter(Objects::nonNull)
                        .max(LocalDateTime::compareTo)
                        .orElse(null))
        );
        return ResponseEntity.ok(status);
    }
}