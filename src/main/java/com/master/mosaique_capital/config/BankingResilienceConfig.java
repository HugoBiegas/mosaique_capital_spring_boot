// com/master/mosaique_capital/config/BankingResilienceConfig.java
package com.master.mosaique_capital.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuration Resilience4j pour les services bancaires
 * Implémente les patterns recommandés pour la robustesse des APIs financières
 */
@Configuration
@Slf4j
public class BankingResilienceConfig {

    @Value("${app.banking.circuit-breaker.enabled:true}")
    private boolean circuitBreakerEnabled;

    @Value("${app.banking.retry.enabled:true}")
    private boolean retryEnabled;

    @Value("${app.banking.rate-limiter.enabled:true}")
    private boolean rateLimiterEnabled;

    /**
     * Configuration du Circuit Breaker pour les APIs bancaires
     * Évite les appels en cascade lors de pannes
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                // Seuil d'échec pour ouvrir le circuit (50%)
                .failureRateThreshold(50.0f)
                // Nombre minimum d'appels avant évaluation
                .minimumNumberOfCalls(10)
                // Taille de la fenêtre glissante
                .slidingWindowSize(20)
                // Durée en état ouvert avant test
                .waitDurationInOpenState(Duration.ofSeconds(30))
                // Nombre d'appels autorisés en état semi-ouvert
                .permittedNumberOfCallsInHalfOpenState(5)
                // Seuil de lenteur (10 secondes pour les APIs bancaires)
                .slowCallRateThreshold(80.0f)
                .slowCallDurationThreshold(Duration.ofSeconds(10))
                // Enregistrer les exceptions qui doivent ouvrir le circuit
                .recordExceptions(
                        org.springframework.web.client.ResourceAccessException.class,
                        org.springframework.web.client.HttpServerErrorException.class,
                        java.net.SocketTimeoutException.class,
                        com.master.mosaique_capital.exception.BankConnectionException.class
                )
                // Ignorer les erreurs client (4xx)
                .ignoreExceptions(
                        org.springframework.web.client.HttpClientErrorException.class,
                        com.master.mosaique_capital.exception.InvalidCredentialsException.class
                )
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);

        // Configuration spécifique par provider
        configureProviderCircuitBreakers(registry);

        log.info("✅ Circuit Breaker banking configuré: failureRate={}%, windowSize={}",
                config.getFailureRateThreshold(), config.getSlidingWindowSize());

        return registry;
    }

    /**
     * Configuration du Retry avec backoff exponentiel
     * ✅ CORRECTION : Utilisation uniquement d'intervalFunction (pas de waitDuration)
     */
    @Bean
    public RetryRegistry retryRegistry() {
        RetryConfig config = RetryConfig.custom()
                // 3 tentatives maximum
                .maxAttempts(3)
                .intervalFunction(attempt -> {
                    // Délai initial de 1 seconde, puis exponentiel (1s, 2s, 4s)
                    long delayMs = 1000L * (long) Math.pow(2, attempt - 1);
                    log.debug("Retry attempt {}: délai = {}ms", attempt, delayMs);
                    return delayMs;
                })
                // Exceptions déclenchant un retry
                .retryOnException(throwable -> {
                    if (throwable instanceof org.springframework.web.client.ResourceAccessException ||
                            throwable instanceof java.net.SocketTimeoutException ||
                            throwable instanceof org.springframework.web.client.HttpServerErrorException) {
                        return true;
                    }
                    if (throwable instanceof com.master.mosaique_capital.exception.BankConnectionException &&
                            throwable.getMessage().contains("timeout")) {
                        return true;
                    }
                    return false;
                })
                // Exceptions à ne pas retry
                .ignoreExceptions(
                        org.springframework.web.client.HttpClientErrorException.Unauthorized.class,
                        com.master.mosaique_capital.exception.InvalidCredentialsException.class
                )
                .build();

        RetryRegistry registry = RetryRegistry.of(config);

        log.info("✅ Retry banking configuré: maxAttempts={}, backoff=exponentiel",
                config.getMaxAttempts());

        return registry;
    }

    /**
     * Configuration du Rate Limiter pour respecter les quotas des APIs
     * Évite de dépasser les limites imposées par les providers
     */
    @Bean
    public RateLimiterRegistry rateLimiterRegistry() {
        // Configuration générale conservatrice
        RateLimiterConfig config = RateLimiterConfig.custom()
                // 50 requêtes par minute (respecte la plupart des APIs)
                .limitForPeriod(50)
                .limitRefreshPeriod(Duration.ofMinutes(1))
                // Timeout si limite atteinte
                .timeoutDuration(Duration.ofSeconds(5))
                .build();

        RateLimiterRegistry registry = RateLimiterRegistry.of(config);

        // Configuration spécifique par provider
        configureProviderRateLimiters(registry);

        log.info("✅ Rate Limiter banking configuré: {}req/min, timeout={}s",
                config.getLimitForPeriod(), config.getTimeoutDuration().getSeconds());

        return registry;
    }

    /**
     * Configuration du Time Limiter pour les opérations asynchrones
     */
    @Bean
    public TimeLimiterConfig timeLimiterConfig() {
        TimeLimiterConfig config = TimeLimiterConfig.custom()
                // Timeout global de 60 secondes pour les opérations bancaires
                .timeoutDuration(Duration.ofSeconds(60))
                // Annuler la tâche en cas de timeout
                .cancelRunningFuture(true)
                .build();

        log.info("✅ Time Limiter banking configuré: timeout={}s",
                config.getTimeoutDuration().getSeconds());

        return config;
    }

    /**
     * Configuration spécifique des Circuit Breakers par provider
     */
    private void configureProviderCircuitBreakers(CircuitBreakerRegistry registry) {
        // Budget Insight - Plus tolérant (historiquement stable)
        CircuitBreakerConfig budgetInsightConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(60.0f) // Plus tolérant
                .minimumNumberOfCalls(15)
                .slidingWindowSize(30)
                .waitDurationInOpenState(Duration.ofSeconds(45))
                .build();
        registry.circuitBreaker("budget-insight", budgetInsightConfig);

        // Bridge API - Configuration standard (API moderne)
        CircuitBreakerConfig bridgeConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50.0f)
                .minimumNumberOfCalls(10)
                .slidingWindowSize(20)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .build();
        registry.circuitBreaker("bridge-api", bridgeConfig);

        // Linxo - Plus strict (peut être moins stable)
        CircuitBreakerConfig linxoConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(40.0f) // Plus strict
                .minimumNumberOfCalls(8)
                .slidingWindowSize(15)
                .waitDurationInOpenState(Duration.ofSeconds(60))
                .build();
        registry.circuitBreaker("linxo", linxoConfig);

        log.info("🔧 Circuit Breakers spécifiques configurés pour chaque provider bancaire");
    }

    /**
     * Configuration spécifique des Rate Limiters par provider
     */
    private void configureProviderRateLimiters(RateLimiterRegistry registry) {
        // Budget Insight - Limite généreuse (API enterprise)
        RateLimiterConfig budgetInsightConfig = RateLimiterConfig.custom()
                .limitForPeriod(100) // 100 req/min
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .timeoutDuration(Duration.ofSeconds(3))
                .build();
        registry.rateLimiter("budget-insight", budgetInsightConfig);

        // Bridge API - Limite modérée
        RateLimiterConfig bridgeConfig = RateLimiterConfig.custom()
                .limitForPeriod(75) // 75 req/min
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .timeoutDuration(Duration.ofSeconds(5))
                .build();
        registry.rateLimiter("bridge-api", bridgeConfig);

        // Linxo - Limite conservatrice
        RateLimiterConfig linxoConfig = RateLimiterConfig.custom()
                .limitForPeriod(30) // 30 req/min (plus strict)
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .timeoutDuration(Duration.ofSeconds(8))
                .build();
        registry.rateLimiter("linxo", linxoConfig);

        // Tink - Limite freemium
        RateLimiterConfig tinkConfig = RateLimiterConfig.custom()
                .limitForPeriod(20) // 20 req/min (freemium)
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .timeoutDuration(Duration.ofSeconds(10))
                .build();
        registry.rateLimiter("tink", tinkConfig);

        log.info("🚦 Rate Limiters spécifiques configurés pour chaque provider bancaire");
    }

    /**
     * Beans nommés pour injection spécifique
     */
    @Bean("budgetInsightCircuitBreaker")
    public CircuitBreaker budgetInsightCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("budget-insight");
    }

    @Bean("bridgeApiCircuitBreaker")
    public CircuitBreaker bridgeApiCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("bridge-api");
    }

    @Bean("bankingRetry")
    public Retry bankingRetry(RetryRegistry registry) {
        return registry.retry("banking");
    }

    @Bean("bankingRateLimiter")
    public RateLimiter bankingRateLimiter(RateLimiterRegistry registry) {
        return registry.rateLimiter("banking");
    }
}