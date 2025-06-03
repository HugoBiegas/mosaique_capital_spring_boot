// com/master/mosaique_capital/service/banking/external/BudgetInsightService.java
package com.master.mosaique_capital.service.banking.external;

import com.master.mosaique_capital.dto.banking.BankConnectionRequest;
import com.master.mosaique_capital.dto.banking.external.ExternalAccountDto;
import com.master.mosaique_capital.dto.banking.external.ExternalTransactionDto;
import com.master.mosaique_capital.exception.BankConnectionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Service d'intégration avec Budget Insight
 * API Documentation: https://docs.budget-insight.com/
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BudgetInsightService {

    private final RestTemplate restTemplate;

    @Value("${app.banking.budget-insight.api-url:https://demo.biapi.pro/2.0}")
    private String apiUrl;

    @Value("${app.banking.budget-insight.client-id}")
    private String clientId;

    @Value("${app.banking.budget-insight.client-secret}")
    private String clientSecret;

    @Value("${app.banking.budget-insight.enabled:true}")
    private boolean enabled;

    /**
     * Initie une connexion bancaire via Budget Insight
     */
    public String initiateConnection(BankConnectionRequest.BankCredentials credentials) {
        if (!enabled) {
            throw new BankConnectionException("Budget Insight n'est pas activé");
        }

        log.info("Initiation connexion Budget Insight pour l'utilisateur: {}", credentials.getLogin());

        try {
            // 1. Obtenir un token d'accès
            String accessToken = getAccessToken();

            // 2. Créer un utilisateur temporaire
            String tempUserId = createTemporaryUser(accessToken);

            // 3. Initier la connexion bancaire
            String connectionId = initiateUserConnection(accessToken, tempUserId, credentials);

            log.info("Connexion Budget Insight initiée avec succès. Connection ID: {}", connectionId);
            return "bi_" + connectionId;

        } catch (Exception e) {
            log.error("Erreur lors de l'initiation de la connexion Budget Insight: {}", e.getMessage(), e);
            throw new BankConnectionException("Impossible d'initier la connexion Budget Insight: " + e.getMessage());
        }
    }

    /**
     * Confirme une connexion après authentification forte
     */
    public boolean confirmConnection(String connectionId, String confirmationCode) {
        if (!enabled) {
            return false;
        }

        // Extraction de l'ID réel (sans le préfixe bi_)
        String realConnectionId = connectionId.replace("bi_", "");

        log.info("Confirmation connexion Budget Insight: {}", realConnectionId);

        try {
            String accessToken = getAccessToken();

            HttpHeaders headers = createAuthHeaders(accessToken);

            Map<String, Object> confirmationData = Map.of(
                    "code", confirmationCode
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(confirmationData, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    apiUrl + "/users/{userId}/connections/{connectionId}/confirm",
                    HttpMethod.POST,
                    request,
                    Map.class,
                    extractUserIdFromConnectionId(realConnectionId),
                    realConnectionId
            );

            boolean success = response.getStatusCode() == HttpStatus.OK;
            log.info("Confirmation Budget Insight: {}", success ? "succès" : "échec");

            return success;

        } catch (Exception e) {
            log.error("Erreur lors de la confirmation Budget Insight: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Récupère les comptes d'une connexion
     */
    public List<ExternalAccountDto> getAccounts(String connectionId) {
        if (!enabled) {
            return List.of();
        }

        String realConnectionId = connectionId.replace("bi_", "");
        log.info("Récupération des comptes Budget Insight pour la connexion: {}", realConnectionId);

        try {
            String accessToken = getAccessToken();
            String userId = extractUserIdFromConnectionId(realConnectionId);

            HttpHeaders headers = createAuthHeaders(accessToken);
            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    apiUrl + "/users/{userId}/accounts",
                    HttpMethod.GET,
                    request,
                    Map.class,
                    userId
            );

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> accountsData = (List<Map<String, Object>>) response.getBody().get("accounts");

            return accountsData.stream()
                    .map(this::mapToExternalAccount)
                    .toList();

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des comptes Budget Insight: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Récupère les transactions d'un compte
     */
    public List<ExternalTransactionDto> getTransactions(String connectionId, String accountId, int days) {
        if (!enabled) {
            return List.of();
        }

        String realConnectionId = connectionId.replace("bi_", "");
        log.info("Récupération des transactions Budget Insight pour le compte: {} (derniers {} jours)", accountId, days);

        try {
            String accessToken = getAccessToken();
            String userId = extractUserIdFromConnectionId(realConnectionId);

            HttpHeaders headers = createAuthHeaders(accessToken);
            HttpEntity<Void> request = new HttpEntity<>(headers);

            // Calcul de la date de début
            String startDate = java.time.LocalDate.now().minusDays(days).toString();

            ResponseEntity<Map> response = restTemplate.exchange(
                    apiUrl + "/users/{userId}/accounts/{accountId}/transactions?min_date={startDate}",
                    HttpMethod.GET,
                    request,
                    Map.class,
                    userId,
                    accountId,
                    startDate
            );

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> transactionsData = (List<Map<String, Object>>) response.getBody().get("transactions");

            return transactionsData.stream()
                    .map(this::mapToExternalTransaction)
                    .toList();

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des transactions Budget Insight: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Vérifie l'état de santé d'une connexion
     */
    public boolean checkHealth(String connectionId) {
        if (!enabled) {
            return false;
        }

        try {
            String realConnectionId = connectionId.replace("bi_", "");
            String accessToken = getAccessToken();
            String userId = extractUserIdFromConnectionId(realConnectionId);

            HttpHeaders headers = createAuthHeaders(accessToken);
            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    apiUrl + "/users/{userId}/connections/{connectionId}",
                    HttpMethod.GET,
                    request,
                    Map.class,
                    userId,
                    realConnectionId
            );

            Map<String, Object> connectionData = response.getBody();
            String state = (String) connectionData.get("state");

            return "valid".equals(state);

        } catch (Exception e) {
            log.warn("Erreur lors de la vérification de santé Budget Insight: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Révoque une connexion
     */
    public boolean revokeConnection(String connectionId) {
        if (!enabled) {
            return false;
        }

        try {
            String realConnectionId = connectionId.replace("bi_", "");
            String accessToken = getAccessToken();
            String userId = extractUserIdFromConnectionId(realConnectionId);

            HttpHeaders headers = createAuthHeaders(accessToken);
            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<Void> response = restTemplate.exchange(
                    apiUrl + "/users/{userId}/connections/{connectionId}",
                    HttpMethod.DELETE,
                    request,
                    Void.class,
                    userId,
                    realConnectionId
            );

            return response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.NO_CONTENT;

        } catch (Exception e) {
            log.error("Erreur lors de la révocation Budget Insight: {}", e.getMessage(), e);
            return false;
        }
    }

    // ===== Méthodes utilitaires privées =====

    private String getAccessToken() {
        try {
            String credentials = Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Authorization", "Basic " + credentials);

            String body = "grant_type=client_credentials";
            HttpEntity<String> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    apiUrl + "/auth/token",
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            return (String) response.getBody().get("access_token");

        } catch (Exception e) {
            throw new BankConnectionException("Impossible d'obtenir le token Budget Insight: " + e.getMessage());
        }
    }

    private String createTemporaryUser(String accessToken) {
        HttpHeaders headers = createAuthHeaders(accessToken);

        Map<String, Object> userData = Map.of(
                "login", "temp_user_" + System.currentTimeMillis()
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(userData, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                apiUrl + "/users",
                HttpMethod.POST,
                request,
                Map.class
        );

        return response.getBody().get("id").toString();
    }

    private String initiateUserConnection(String accessToken, String userId, BankConnectionRequest.BankCredentials credentials) {
        HttpHeaders headers = createAuthHeaders(accessToken);

        Map<String, Object> connectionData = Map.of(
                "login", credentials.getLogin(),
                "password", credentials.getPassword()
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(connectionData, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                apiUrl + "/users/{userId}/connections",
                HttpMethod.POST,
                request,
                Map.class,
                userId
        );

        return response.getBody().get("id").toString();
    }

    private HttpHeaders createAuthHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        return headers;
    }

    private String extractUserIdFromConnectionId(String connectionId) {
        // Pour la démo, on utilise un mapping simple
        // En production, il faudrait stocker cette relation
        return "temp_user_" + connectionId.hashCode();
    }

    private ExternalAccountDto mapToExternalAccount(Map<String, Object> accountData) {
        return ExternalAccountDto.builder()
                .externalId(accountData.get("id").toString())
                .name((String) accountData.get("name"))
                .type((String) accountData.get("type"))
                .balance(new java.math.BigDecimal(accountData.get("balance").toString()))
                .currency((String) accountData.get("currency"))
                .iban((String) accountData.get("iban"))
                .status((String) accountData.get("status"))
                .build();
    }

    private ExternalTransactionDto mapToExternalTransaction(Map<String, Object> transactionData) {
        return ExternalTransactionDto.builder()
                .externalId(transactionData.get("id").toString())
                .amount(new java.math.BigDecimal(transactionData.get("value").toString()))
                .description((String) transactionData.get("wording"))
                .transactionDate(java.time.LocalDate.parse((String) transactionData.get("date")))
                .category((String) transactionData.get("category"))
                .type(((Number) transactionData.get("value")).doubleValue() >= 0 ? "CREDIT" : "DEBIT")
                .build();
    }
}