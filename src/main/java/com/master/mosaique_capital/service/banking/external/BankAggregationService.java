// ===== BankAggregationService.java =====
package com.master.mosaique_capital.service.banking.external;

import com.master.mosaique_capital.dto.banking.BankConnectionRequest;
import com.master.mosaique_capital.dto.banking.BankProviderDto;
import com.master.mosaique_capital.dto.banking.external.ExternalAccountDto;
import com.master.mosaique_capital.dto.banking.external.ExternalTransactionDto;
import com.master.mosaique_capital.exception.BankConnectionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;

/**
 * Service d'intégration avec les providers d'agrégation bancaire (Budget Insight, Linxo, etc.)
 * Implémente le pattern Strategy pour supporter plusieurs providers
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BankAggregationService {

    private final RestTemplate restTemplate;
    private final BudgetInsightService budgetInsightService;
    private final LinxoService linxoService;

    @Value("${app.banking.default-provider:budget-insight}")
    private String defaultProvider;

    @Value("${app.banking.timeout:30000}")
    private int requestTimeout;

    /**
     * Vérifie si un provider est supporté
     */
    public boolean isSupportedProvider(String provider) {
        return switch (provider.toLowerCase()) {
            case "budget-insight", "bi" -> true;
            case "linxo" -> true;
            case "mock" -> true; // Pour les tests
            default -> false;
        };
    }

    /**
     * Récupère la liste des providers disponibles
     */
    public List<BankProviderDto> getAvailableProviders() {
        return List.of(
                new BankProviderDto("budget-insight", "Budget Insight", "/images/providers/bi.png",
                        true, List.of("login", "password"), "Agrégation bancaire française leader", true),
                new BankProviderDto("linxo", "Linxo", "/images/providers/linxo.png",
                        true, List.of("login", "password"), "Spécialiste PFM français", true),
                new BankProviderDto("mock", "Mock Provider", "/images/providers/mock.png",
                        true, List.of("login", "password"), "Provider de test", false)
        );
    }

    /**
     * Initie une connexion bancaire via le provider spécifié
     */
    public String initiateConnection(String provider, BankConnectionRequest.BankCredentials credentials) {
        log.info("Initiation connexion bancaire via provider: {}", provider);

        return switch (provider.toLowerCase()) {
            case "budget-insight", "bi" -> budgetInsightService.initiateConnection(credentials);
            case "linxo" -> linxoService.initiateConnection(credentials);
            case "mock" -> createMockConnection(credentials);
            default -> throw new BankConnectionException("Provider non supporté: " + provider);
        };
    }

    /**
     * Confirme une connexion bancaire après authentification forte
     */
    public boolean confirmConnection(String connectionId, String confirmationCode) {
        log.info("Confirmation connexion bancaire: {}", connectionId);

        // Détermine le provider basé sur l'ID de connexion
        String provider = determineProviderFromConnectionId(connectionId);

        return switch (provider.toLowerCase()) {
            case "budget-insight", "bi" -> budgetInsightService.confirmConnection(connectionId, confirmationCode);
            case "linxo" -> linxoService.confirmConnection(connectionId, confirmationCode);
            case "mock" -> true; // Mock toujours OK
            default -> throw new BankConnectionException("Provider non supporté pour la confirmation");
        };
    }

    /**
     * Récupère les comptes bancaires d'une connexion
     */
    public List<ExternalAccountDto> getAccounts(String connectionId) {
        log.info("Récupération des comptes pour la connexion: {}", connectionId);

        String provider = determineProviderFromConnectionId(connectionId);

        return switch (provider.toLowerCase()) {
            case "budget-insight", "bi" -> budgetInsightService.getAccounts(connectionId);
            case "linxo" -> linxoService.getAccounts(connectionId);
            case "mock" -> createMockAccounts(connectionId);
            default -> throw new BankConnectionException("Provider non supporté pour la récupération des comptes");
        };
    }

    /**
     * Récupère les transactions d'un compte
     */
    public List<ExternalTransactionDto> getTransactions(String connectionId, String accountId, int days) {
        log.info("Récupération des transactions pour le compte: {} (derniers {} jours)", accountId, days);

        String provider = determineProviderFromConnectionId(connectionId);

        return switch (provider.toLowerCase()) {
            case "budget-insight", "bi" -> budgetInsightService.getTransactions(connectionId, accountId, days);
            case "linxo" -> linxoService.getTransactions(connectionId, accountId, days);
            case "mock" -> createMockTransactions(accountId, days);
            default -> throw new BankConnectionException("Provider non supporté pour la récupération des transactions");
        };
    }

    /**
     * Vérifie l'état de santé d'une connexion
     */
    public boolean checkConnectionHealth(String connectionId) {
        try {
            String provider = determineProviderFromConnectionId(connectionId);

            return switch (provider.toLowerCase()) {
                case "budget-insight", "bi" -> budgetInsightService.checkHealth(connectionId);
                case "linxo" -> linxoService.checkHealth(connectionId);
                case "mock" -> true; // Mock toujours OK
                default -> false;
            };
        } catch (Exception e) {
            log.warn("Erreur lors de la vérification de santé de la connexion {}: {}", connectionId, e.getMessage());
            return false;
        }
    }

    /**
     * Révoque une connexion bancaire
     */
    public boolean revokeConnection(String connectionId) {
        log.info("Révocation de la connexion: {}", connectionId);

        try {
            String provider = determineProviderFromConnectionId(connectionId);

            return switch (provider.toLowerCase()) {
                case "budget-insight", "bi" -> budgetInsightService.revokeConnection(connectionId);
                case "linxo" -> linxoService.revokeConnection(connectionId);
                case "mock" -> true; // Mock toujours OK
                default -> false;
            };
        } catch (Exception e) {
            log.error("Erreur lors de la révocation de la connexion {}: {}", connectionId, e.getMessage());
            return false;
        }
    }

    // ===== Méthodes utilitaires =====

    private String determineProviderFromConnectionId(String connectionId) {
        // Convention: les IDs de connexion sont préfixés par le provider
        if (connectionId.startsWith("bi_")) return "budget-insight";
        if (connectionId.startsWith("linxo_")) return "linxo";
        if (connectionId.startsWith("mock_")) return "mock";

        // Fallback sur le provider par défaut
        return defaultProvider;
    }

    // ===== Méthodes Mock pour les tests =====

    private String createMockConnection(BankConnectionRequest.BankCredentials credentials) {
        return "mock_" + System.currentTimeMillis();
    }

    private List<ExternalAccountDto> createMockAccounts(String connectionId) {
        return List.of(
                ExternalAccountDto.builder()
                        .externalId("mock_account_1")
                        .name("Compte Courant Mock")
                        .type("checking")
                        .balance(java.math.BigDecimal.valueOf(2450.75))
                        .currency("EUR")
                        .iban("FR1420041010050500013M02606")
                        .build(),
                ExternalAccountDto.builder()
                        .externalId("mock_account_2")
                        .name("Livret A Mock")
                        .type("savings")
                        .balance(java.math.BigDecimal.valueOf(15000.00))
                        .currency("EUR")
                        .build()
        );
    }

    private List<ExternalTransactionDto> createMockTransactions(String accountId, int days) {
        return List.of(
                ExternalTransactionDto.builder()
                        .externalId("mock_tx_1")
                        .amount(java.math.BigDecimal.valueOf(-45.67))
                        .description("ACHAT CARTE MONOPRIX")
                        .transactionDate(java.time.LocalDate.now().minusDays(1))
                        .category("alimentation")
                        .type("DEBIT")
                        .build(),
                ExternalTransactionDto.builder()
                        .externalId("mock_tx_2")
                        .amount(java.math.BigDecimal.valueOf(2500.00))
                        .description("SALAIRE ENTREPRISE XYZ")
                        .transactionDate(java.time.LocalDate.now().minusDays(3))
                        .category("salaire")
                        .type("CREDIT")
                        .build()
        );
    }
}

