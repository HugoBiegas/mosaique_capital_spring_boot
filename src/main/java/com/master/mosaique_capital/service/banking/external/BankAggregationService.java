// com/master/mosaique_capital/service/banking/external/BankAggregationService.java
package com.master.mosaique_capital.service.banking.external;

import com.master.mosaique_capital.dto.banking.BankConnectionRequest;
import com.master.mosaique_capital.dto.banking.BankProviderDto;
import com.master.mosaique_capital.dto.banking.external.ExternalAccountDto;
import com.master.mosaique_capital.dto.banking.external.ExternalTransactionDto;
import com.master.mosaique_capital.exception.BankConnectionException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * Service d'intégration avec les providers d'agrégation bancaire
 * Implémente les patterns de résilience recommandés pour la production
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BankAggregationService {

    private final BudgetInsightService budgetInsightService;
    private final BridgeApiService bridgeApiService;
    private final LinxoService linxoService;

    // Registres Resilience4j
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;
    private final RateLimiterRegistry rateLimiterRegistry;

    @Value("${banking.default-provider:budget-insight}")
    private String defaultProvider;

    @Value("${banking.circuit-breaker.enabled:true}")
    private boolean circuitBreakerEnabled;

    @Value("${banking.retry.enabled:true}")
    private boolean retryEnabled;

    @Value("${banking.rate-limiter.enabled:true}")
    private boolean rateLimiterEnabled;

    /**
     * Vérifie si un provider est supporté
     */
    public boolean isSupportedProvider(String provider) {
        return switch (provider.toLowerCase()) {
            case "budget-insight", "bi", "powens" -> true;
            case "bridge", "bridge-api" -> true;
            case "linxo", "linxo-connect" -> true;
            case "tink", "nordigen" -> true;
            case "mock" -> true; // Pour les tests
            default -> false;
        };
    }

    /**
     * Récupère la liste des providers disponibles avec statut temps réel
     */
    public List<BankProviderDto> getAvailableProviders() {
        return List.of(
                createProviderDto("budget-insight", "Budget Insight (Powens)",
                        "/images/providers/budget-insight.png",
                        "Le leader européen avec 1800+ institutions. Plateforme mature avec 99.95% de disponibilité.",
                        checkProviderHealth("budget-insight")),

                createProviderDto("bridge-api", "Bridge API",
                        "/images/providers/bridge.png",
                        "API moderne avec routing intelligent. Traite 1.5Md€ quotidiennement avec excellentes performances.",
                        checkProviderHealth("bridge-api")),

                createProviderDto("linxo-connect", "Linxo Connect",
                        "/images/providers/linxo.png",
                        "Solution Crédit Agricole certifiée ISO 27001. Combine PSD2 et screen scraping.",
                        checkProviderHealth("linxo")),

                createProviderDto("tink", "Tink (ex-Nordigen)",
                        "/images/providers/tink.png",
                        "Modèle freemium attractif avec 2500+ institutions européennes. Idéal pour startups.",
                        false), // Tink non implémenté pour l'instant

                createProviderDto("mock", "Mock Provider",
                        "/images/providers/mock.png",
                        "Provider de test pour développement et démonstration",
                        true)
        );
    }

    /**
     * Initie une connexion bancaire avec patterns de résilience
     */
    public String initiateConnection(String provider, BankConnectionRequest.BankCredentials credentials) {
        log.info("🔄 Initiation connexion bancaire via provider: {} (avec résilience)", provider);

        return executeWithResilience(provider, "initiate", () -> {
            return switch (provider.toLowerCase()) {
                case "budget-insight", "bi", "powens" -> budgetInsightService.initiateConnection(credentials);
                case "bridge", "bridge-api" -> bridgeApiService.initiateConnection(credentials);
                case "linxo", "linxo-connect" -> linxoService.initiateConnection(credentials);
                case "mock" -> createMockConnection(credentials);
                default -> throw new BankConnectionException("Provider non supporté: " + provider);
            };
        });
    }

    /**
     * Confirme une connexion bancaire avec résilience
     */
    public boolean confirmConnection(String connectionId, String confirmationCode) {
        String provider = determineProviderFromConnectionId(connectionId);
        log.info("🔐 Confirmation connexion bancaire: {} via {}", connectionId, provider);

        return executeWithResilience(provider, "confirm", () -> {
            return switch (provider.toLowerCase()) {
                case "budget-insight", "bi", "powens" -> budgetInsightService.confirmConnection(connectionId, confirmationCode);
                case "bridge", "bridge-api" -> bridgeApiService.confirmConnection(connectionId, confirmationCode);
                case "linxo", "linxo-connect" -> linxoService.confirmConnection(connectionId, confirmationCode);
                case "mock" -> true;
                default -> throw new BankConnectionException("Provider non supporté pour confirmation: " + provider);
            };
        });
    }

    /**
     * Récupère les comptes bancaires avec résilience
     */
    public List<ExternalAccountDto> getAccounts(String connectionId) {
        String provider = determineProviderFromConnectionId(connectionId);
        log.info("📋 Récupération des comptes pour connexion: {} via {}", connectionId, provider);

        return executeWithResilience(provider, "accounts", () -> {
            return switch (provider.toLowerCase()) {
                case "budget-insight", "bi", "powens" -> budgetInsightService.getAccounts(connectionId);
                case "bridge", "bridge-api" -> bridgeApiService.getAccounts(connectionId);
                case "linxo", "linxo-connect" -> linxoService.getAccounts(connectionId);
                case "mock" -> createMockAccounts(connectionId);
                default -> {
                    log.warn("Provider non supporté pour getAccounts: {}", provider);
                    yield Collections.emptyList();
                }
            };
        });
    }

    /**
     * Récupère les transactions avec résilience
     */
    public List<ExternalTransactionDto> getTransactions(String connectionId, String accountId, int days) {
        String provider = determineProviderFromConnectionId(connectionId);
        log.info("💰 Récupération transactions compte: {} ({}j) via {}", accountId, days, provider);

        return executeWithResilience(provider, "transactions", () -> {
            return switch (provider.toLowerCase()) {
                case "budget-insight", "bi", "powens" -> budgetInsightService.getTransactions(connectionId, accountId, days);
                case "bridge", "bridge-api" -> bridgeApiService.getTransactions(connectionId, accountId, days);
                case "linxo", "linxo-connect" -> linxoService.getTransactions(connectionId, accountId, days);
                case "mock" -> createMockTransactions(accountId, days);
                default -> {
                    log.warn("Provider non supporté pour getTransactions: {}", provider);
                    yield Collections.emptyList();
                }
            };
        });
    }

    /**
     * Vérifie l'état de santé d'une connexion avec résilience
     */
    public boolean checkConnectionHealth(String connectionId) {
        String provider = determineProviderFromConnectionId(connectionId);

        try {
            return executeWithResilience(provider, "health", () -> {
                return switch (provider.toLowerCase()) {
                    case "budget-insight", "bi", "powens" -> budgetInsightService.checkHealth(connectionId);
                    case "bridge", "bridge-api" -> bridgeApiService.checkHealth(connectionId);
                    case "linxo", "linxo-connect" -> linxoService.checkHealth(connectionId);
                    case "mock" -> true;
                    default -> false;
                };
            });
        } catch (Exception e) {
            log.warn("Erreur lors de la vérification de santé de la connexion {}: {}", connectionId, e.getMessage());
            return false;
        }
    }

    /**
     * Révoque une connexion bancaire avec résilience
     */
    public void revokeConnection(String connectionId) {
        String provider = determineProviderFromConnectionId(connectionId);
        log.info("🗑️ Révocation connexion: {} via {}", connectionId, provider);

        try {
            executeWithResilience(provider, "revoke", () -> {
                return switch (provider.toLowerCase()) {
                    case "budget-insight", "bi", "powens" -> budgetInsightService.revokeConnection(connectionId);
                    case "bridge", "bridge-api" -> bridgeApiService.revokeConnection(connectionId);
                    case "linxo", "linxo-connect" -> linxoService.revokeConnection(connectionId);
                    case "mock" -> true;
                    default -> false;
                };
            });
        } catch (Exception e) {
            log.error("Erreur lors de la révocation de la connexion {}: {}", connectionId, e.getMessage());
        }
    }

    // ===== Méthodes de résilience =====

    /**
     * Exécute une opération avec tous les patterns de résilience activés
     */
    private <T> T executeWithResilience(String provider, String operation, Supplier<T> supplier) {
        Supplier<T> decoratedSupplier = supplier;

        // Application des patterns de résilience si activés
        if (rateLimiterEnabled) {
            RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter(provider, provider);
            decoratedSupplier = RateLimiter.decorateSupplier(rateLimiter, decoratedSupplier);
            log.debug("🚦 Rate Limiter appliqué pour {}.{}", provider, operation);
        }

        if (retryEnabled) {
            Retry retry = retryRegistry.retry(provider, "banking");
            decoratedSupplier = Retry.decorateSupplier(retry, decoratedSupplier);
            log.debug("🔄 Retry appliqué pour {}.{}", provider, operation);
        }

        if (circuitBreakerEnabled) {
            CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(provider, provider);
            decoratedSupplier = CircuitBreaker.decorateSupplier(circuitBreaker, decoratedSupplier);
            log.debug("⚡ Circuit Breaker appliqué pour {}.{}", provider, operation);
        }

        try {
            T result = decoratedSupplier.get();
            log.debug("✅ Opération {}.{} exécutée avec succès", provider, operation);
            return result;
        } catch (Exception e) {
            log.error("❌ Échec opération {}.{}: {}", provider, operation, e.getMessage());
            throw e;
        }
    }

    /**
     * Vérifie la santé d'un provider spécifique
     */
    private boolean checkProviderHealth(String provider) {
        try {
            return switch (provider.toLowerCase()) {
                case "budget-insight" -> budgetInsightService.checkHealth("health-check");
                case "bridge-api" -> bridgeApiService.checkHealth("health-check");
                case "linxo" -> linxoService.checkHealth("health-check");
                default -> false;
            };
        } catch (Exception e) {
            log.debug("Provider {} indisponible: {}", provider, e.getMessage());
            return false;
        }
    }

    // ===== Méthodes utilitaires =====

    /**
     * Détermine le provider depuis l'ID de connexion
     */
    private String determineProviderFromConnectionId(String connectionId) {
        if (connectionId.startsWith("bi_")) return "budget-insight";
        if (connectionId.startsWith("bridge_")) return "bridge-api";
        if (connectionId.startsWith("linxo_")) return "linxo";
        if (connectionId.startsWith("tink_")) return "tink";
        if (connectionId.startsWith("mock_")) return "mock";

        // Fallback sur le provider par défaut
        log.warn("⚠️ Format de connectionId non reconnu: {}, utilisation du provider par défaut", connectionId);
        return defaultProvider;
    }

    /**
     * Crée un DTO de provider avec informations complètes
     */
    private BankProviderDto createProviderDto(String code, String name, String logo, String description, boolean available) {
        return new BankProviderDto(
                code,
                name,
                logo,
                available,
                List.of("login", "password"), // Champs standard
                description,
                true // Authentification forte supportée
        );
    }

    // ===== Méthodes Mock (inchangées) =====

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