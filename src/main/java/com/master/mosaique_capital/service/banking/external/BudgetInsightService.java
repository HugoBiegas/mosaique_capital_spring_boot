// src/main/java/com/master/mosaique_capital/service/banking/external/BudgetInsightService.java
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
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service d'intégration réelle avec Budget Insight (Powens)
 * Documentation API: https://docs.budget-insight.com/
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BudgetInsightService {

    private final RestTemplate restTemplate;

    // Cache pour les tokens d'accès
    private final Map<String, TokenInfo> tokenCache = new ConcurrentHashMap<>();

    @Value("${app.banking.budget-insight.api-url:https://demo.biapi.pro/2.0}")
    private String apiUrl;

    @Value("${app.banking.budget-insight.client-id}")
    private String clientId;

    @Value("${app.banking.budget-insight.client-secret}")
    private String clientSecret;

    @Value("${app.banking.budget-insight.enabled:true}")
    private boolean enabled;

    @Value("${app.banking.budget-insight.sandbox:true}")
    private boolean sandboxMode;

    /**
     * Initie une connexion bancaire via Budget Insight
     */
    public String initiateConnection(BankConnectionRequest.BankCredentials credentials) {
        if (!enabled) {
            throw new BankConnectionException("Budget Insight n'est pas activé");
        }

        log.info("🔄 Initiation connexion Budget Insight pour l'utilisateur: {}",
                maskSensitiveData(credentials.getLogin()));

        try {
            // 1. Obtenir un token d'accès
            String accessToken = getOrRefreshAccessToken();

            // 2. Créer un utilisateur temporaire
            String tempUserId = createTemporaryUser(accessToken);

            // 3. Récupérer les connecteurs disponibles
            List<Map<String, Object>> connectors = getAvailableConnectors(accessToken);

            // 4. Détecter automatiquement la banque depuis l'email/login
            String connectorId = detectBankConnector(credentials.getLogin(), connectors);

            // 5. Initier la connexion bancaire
            String connectionId = initiateUserConnection(accessToken, tempUserId, connectorId, credentials);

            log.info("✅ Connexion Budget Insight initiée. Connection ID: {}", connectionId);
            return "bi_" + connectionId + "_" + tempUserId;

        } catch (Exception e) {
            log.error("❌ Erreur lors de l'initiation Budget Insight: {}", e.getMessage(), e);
            throw new BankConnectionException("Échec de la connexion bancaire: " + e.getMessage());
        }
    }

    /**
     * Confirme une connexion après authentification forte
     */
    public boolean confirmConnection(String connectionId, String confirmationCode) {
        if (!enabled) {
            log.warn("Budget Insight désactivé, impossible de confirmer la connexion");
            return false;
        }

        // Parse du connectionId composite: bi_<realConnectionId>_<userId>
        String[] parts = connectionId.replace("bi_", "").split("_", 2);
        if (parts.length != 2) {
            log.error("Format de connectionId invalide: {}", connectionId);
            return false;
        }

        String realConnectionId = parts[0];
        String userId = parts[1];

        log.info("🔐 Confirmation connexion Budget Insight: {} pour utilisateur: {}", realConnectionId, userId);

        try {
            String accessToken = getOrRefreshAccessToken();

            HttpHeaders headers = createAuthHeaders(accessToken);

            Map<String, Object> confirmationData = new HashMap<>();

            // Si un code de confirmation est fourni, l'inclure
            if (confirmationCode != null && !confirmationCode.trim().isEmpty()) {
                confirmationData.put("resume", confirmationCode);
            }

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(confirmationData, headers);

            // Endpoint pour continuer/confirmer la connexion
            String url = String.format("%s/users/%s/connections/%s", apiUrl, userId, realConnectionId);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    request,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> connectionData = response.getBody();
                String state = (String) connectionData.get("state");

                boolean isActive = "valid".equals(state) || "SCARequired".equals(state);
                log.info("✅ Confirmation Budget Insight: {} - État: {}", isActive ? "succès" : "échec", state);

                return isActive;
            }

            return false;

        } catch (RestClientException e) {
            log.error("❌ Erreur réseau lors de la confirmation Budget Insight: {}", e.getMessage(), e);
            return false;
        } catch (Exception e) {
            log.error("❌ Erreur inattendue lors de la confirmation Budget Insight: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Récupère les comptes d'une connexion
     */
    public List<ExternalAccountDto> getAccounts(String connectionId) {
        if (!enabled) {
            log.warn("Budget Insight désactivé");
            return Collections.emptyList();
        }

        String[] parts = connectionId.replace("bi_", "").split("_", 2);
        if (parts.length != 2) {
            log.error("Format de connectionId invalide pour getAccounts: {}", connectionId);
            return Collections.emptyList();
        }

        String userId = parts[1];

        log.info("📋 Récupération comptes Budget Insight pour utilisateur: {}", userId);

        try {
            String accessToken = getOrRefreshAccessToken();

            HttpHeaders headers = createAuthHeaders(accessToken);
            HttpEntity<Void> request = new HttpEntity<>(headers);

            String url = String.format("%s/users/%s/accounts", apiUrl, userId);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> accountsData = (List<Map<String, Object>>)
                        response.getBody().get("accounts");

                if (accountsData == null) {
                    log.warn("Aucune donnée de compte trouvée dans la réponse");
                    return Collections.emptyList();
                }

                List<ExternalAccountDto> accounts = accountsData.stream()
                        .map(this::mapToExternalAccount)
                        .filter(Objects::nonNull)
                        .toList();

                log.info("✅ {} comptes récupérés avec succès", accounts.size());
                return accounts;
            }

            log.warn("Réponse inattendue lors de la récupération des comptes: {}", response.getStatusCode());
            return Collections.emptyList();

        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération des comptes Budget Insight: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Récupère les transactions d'un compte
     */
    public List<ExternalTransactionDto> getTransactions(String connectionId, String accountId, int days) {
        if (!enabled) {
            return Collections.emptyList();
        }

        String[] parts = connectionId.replace("bi_", "").split("_", 2);
        if (parts.length != 2) {
            log.error("Format de connectionId invalide pour getTransactions: {}", connectionId);
            return Collections.emptyList();
        }

        String userId = parts[1];

        log.info("💰 Récupération transactions Budget Insight - Compte: {} (derniers {} jours)",
                accountId, days);

        try {
            String accessToken = getOrRefreshAccessToken();

            HttpHeaders headers = createAuthHeaders(accessToken);
            HttpEntity<Void> request = new HttpEntity<>(headers);

            // Calcul des dates
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(days);

            String url = String.format(
                    "%s/users/%s/accounts/%s/transactions?min_date=%s&max_date=%s&limit=100",
                    apiUrl, userId, accountId,
                    startDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    endDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
            );

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> transactionsData = (List<Map<String, Object>>)
                        response.getBody().get("transactions");

                if (transactionsData == null) {
                    log.info("Aucune transaction trouvée pour le compte: {}", accountId);
                    return Collections.emptyList();
                }

                List<ExternalTransactionDto> transactions = transactionsData.stream()
                        .map(this::mapToExternalTransaction)
                        .filter(Objects::nonNull)
                        .toList();

                log.info("✅ {} transactions récupérées pour le compte: {}", transactions.size(), accountId);
                return transactions;
            }

            return Collections.emptyList();

        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération des transactions Budget Insight: {}", e.getMessage(), e);
            return Collections.emptyList();
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
            String[] parts = connectionId.replace("bi_", "").split("_", 2);
            if (parts.length != 2) {
                return false;
            }

            String realConnectionId = parts[0];
            String userId = parts[1];

            String accessToken = getOrRefreshAccessToken();
            HttpHeaders headers = createAuthHeaders(accessToken);
            HttpEntity<Void> request = new HttpEntity<>(headers);

            String url = String.format("%s/users/%s/connections/%s", apiUrl, userId, realConnectionId);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String state = (String) response.getBody().get("state");
                boolean isHealthy = "valid".equals(state);

                log.debug("🏥 Santé connexion Budget Insight {}: {} (état: {})",
                        connectionId, isHealthy ? "OK" : "KO", state);
                return isHealthy;
            }

            return false;

        } catch (Exception e) {
            log.warn("⚠️ Erreur lors de la vérification de santé Budget Insight: {}", e.getMessage());
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
            String[] parts = connectionId.replace("bi_", "").split("_", 2);
            if (parts.length != 2) {
                return false;
            }

            String realConnectionId = parts[0];
            String userId = parts[1];

            String accessToken = getOrRefreshAccessToken();
            HttpHeaders headers = createAuthHeaders(accessToken);
            HttpEntity<Void> request = new HttpEntity<>(headers);

            String url = String.format("%s/users/%s/connections/%s", apiUrl, userId, realConnectionId);

            ResponseEntity<Void> response = restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    request,
                    Void.class
            );

            boolean revoked = response.getStatusCode().is2xxSuccessful();
            log.info("🗑️ Révocation connexion Budget Insight {}: {}", connectionId, revoked ? "OK" : "KO");

            return revoked;

        } catch (Exception e) {
            log.error("❌ Erreur lors de la révocation Budget Insight: {}", e.getMessage(), e);
            return false;
        }
    }

    // ===== Méthodes utilitaires privées =====

    /**
     * Obtient ou rafraîchit le token d'accès
     */
    private String getOrRefreshAccessToken() {
        try {
            // Vérifier le cache
            TokenInfo cachedToken = tokenCache.get("access_token");
            if (cachedToken != null && !cachedToken.isExpired()) {
                return cachedToken.getToken();
            }

            // Obtenir un nouveau token
            String credentials = Base64.getEncoder().encodeToString(
                    (clientId + ":" + clientSecret).getBytes()
            );

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

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String accessToken = (String) response.getBody().get("access_token");
                Integer expiresIn = (Integer) response.getBody().get("expires_in");

                // Mettre en cache avec une marge de sécurité de 10%
                long expiryTime = System.currentTimeMillis() + (expiresIn * 900L); // 90% de la durée
                tokenCache.put("access_token", new TokenInfo(accessToken, expiryTime));

                log.debug("🔑 Nouveau token Budget Insight obtenu, expire dans {}s", expiresIn);
                return accessToken;
            }

            throw new BankConnectionException("Impossible d'obtenir le token Budget Insight");

        } catch (Exception e) {
            log.error("❌ Erreur lors de l'obtention du token Budget Insight: {}", e.getMessage(), e);
            throw new BankConnectionException("Échec de l'authentification Budget Insight: " + e.getMessage());
        }
    }

    /**
     * Crée un utilisateur temporaire
     */
    private String createTemporaryUser(String accessToken) {
        HttpHeaders headers = createAuthHeaders(accessToken);

        Map<String, Object> userData = new HashMap<>();
        userData.put("login", "mosaique_user_" + System.currentTimeMillis());
        userData.put("password", "temp_password_" + UUID.randomUUID().toString().substring(0, 8));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(userData, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                apiUrl + "/users",
                HttpMethod.POST,
                request,
                Map.class
        );

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            Object userId = response.getBody().get("id");
            log.debug("👤 Utilisateur temporaire créé: {}", userId);
            return userId.toString();
        }

        throw new BankConnectionException("Impossible de créer l'utilisateur temporaire");
    }

    /**
     * Récupère les connecteurs disponibles
     */
    private List<Map<String, Object>> getAvailableConnectors(String accessToken) {
        HttpHeaders headers = createAuthHeaders(accessToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                apiUrl + "/banks",
                HttpMethod.GET,
                request,
                Map.class
        );

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> banks = (List<Map<String, Object>>) response.getBody().get("banks");
            return banks != null ? banks : Collections.emptyList();
        }

        return Collections.emptyList();
    }

    /**
     * Détecte automatiquement le connecteur de banque
     */
    private String detectBankConnector(String login, List<Map<String, Object>> connectors) {
        // Logique de détection basée sur le domaine email ou des patterns
        String loginLower = login.toLowerCase();

        for (Map<String, Object> connector : connectors) {
            String name = (String) connector.get("name");
            if (name == null) continue;

            String nameLower = name.toLowerCase();

            // Correspondances par nom de banque
            if (loginLower.contains("@creditagricole") && nameLower.contains("crédit agricole")) {
                return connector.get("id").toString();
            }
            if (loginLower.contains("@bnpparibas") && nameLower.contains("bnp")) {
                return connector.get("id").toString();
            }
            if (loginLower.contains("@societegenerale") && nameLower.contains("société générale")) {
                return connector.get("id").toString();
            }
            if (loginLower.contains("@lcl") && nameLower.contains("lcl")) {
                return connector.get("id").toString();
            }
        }

        // Fallback sur la première banque disponible (pour sandbox/demo)
        if (!connectors.isEmpty()) {
            Object connectorId = connectors.get(0).get("id");
            log.info("🏦 Utilisation du connecteur par défaut: {}",
                    connectors.get(0).get("name"));
            return connectorId.toString();
        }

        throw new BankConnectionException("Aucun connecteur bancaire disponible");
    }

    /**
     * Initie la connexion utilisateur avec une banque
     */
    private String initiateUserConnection(String accessToken, String userId, String connectorId,
                                          BankConnectionRequest.BankCredentials credentials) {
        HttpHeaders headers = createAuthHeaders(accessToken);

        Map<String, Object> connectionData = new HashMap<>();
        connectionData.put("id_connector", connectorId);
        connectionData.put("login", credentials.getLogin());
        connectionData.put("password", credentials.getPassword());

        // Champs additionnels si fournis
        if (credentials.getAdditionalField1() != null) {
            connectionData.put("field1", credentials.getAdditionalField1());
        }
        if (credentials.getAdditionalField2() != null) {
            connectionData.put("field2", credentials.getAdditionalField2());
        }

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(connectionData, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                String.format("%s/users/%s/connections", apiUrl, userId),
                HttpMethod.POST,
                request,
                Map.class
        );

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            Object connectionId = response.getBody().get("id");
            return connectionId.toString();
        }

        throw new BankConnectionException("Impossible de créer la connexion bancaire");
    }

    /**
     * Crée les headers d'authentification
     */
    private HttpHeaders createAuthHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        return headers;
    }

    /**
     * Mappe les données de compte de Budget Insight vers notre DTO
     */
    private ExternalAccountDto mapToExternalAccount(Map<String, Object> accountData) {
        try {
            // Budget Insight retourne les montants en "milliunits" (diviser par 100)
            Object balanceObj = accountData.get("balance");
            BigDecimal balance = balanceObj != null ?
                    new BigDecimal(balanceObj.toString()).divide(new BigDecimal("100")) :
                    BigDecimal.ZERO;

            return ExternalAccountDto.builder()
                    .externalId(accountData.get("id").toString())
                    .name((String) accountData.get("name"))
                    .type(mapAccountType((String) accountData.get("type")))
                    .balance(balance)
                    .currency((String) accountData.getOrDefault("currency", "EUR"))
                    .iban((String) accountData.get("iban"))
                    .status(accountData.getOrDefault("disabled", false).equals(false) ? "ACTIVE" : "INACTIVE")
                    .build();
        } catch (Exception e) {
            log.error("❌ Erreur lors du mapping du compte: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Mappe les données de transaction de Budget Insight vers notre DTO
     */
    private ExternalTransactionDto mapToExternalTransaction(Map<String, Object> transactionData) {
        try {
            // Budget Insight retourne les montants en "milliunits"
            Object valueObj = transactionData.get("value");
            BigDecimal amount = valueObj != null ?
                    new BigDecimal(valueObj.toString()).divide(new BigDecimal("100")) :
                    BigDecimal.ZERO;

            String dateStr = (String) transactionData.get("date");
            LocalDate transactionDate = dateStr != null ?
                    LocalDate.parse(dateStr) : LocalDate.now();

            String valueDateStr = (String) transactionData.get("value_date");
            LocalDate valueDate = valueDateStr != null ?
                    LocalDate.parse(valueDateStr) : transactionDate;

            return ExternalTransactionDto.builder()
                    .externalId(transactionData.get("id").toString())
                    .amount(amount)
                    .description((String) transactionData.get("wording"))
                    .transactionDate(transactionDate)
                    .valueDate(valueDate)
                    .category(mapTransactionCategory((String) transactionData.get("category")))
                    .type(amount.compareTo(BigDecimal.ZERO) >= 0 ? "CREDIT" : "DEBIT")
                    .build();
        } catch (Exception e) {
            log.error("❌ Erreur lors du mapping de la transaction: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Mappe les types de compte Budget Insight vers nos types
     */
    private String mapAccountType(String biType) {
        if (biType == null) return "checking";

        return switch (biType.toLowerCase()) {
            case "checking", "transaction" -> "checking";
            case "savings", "livret" -> "savings";
            case "loan", "credit" -> "loan";
            case "card" -> "card";
            case "insurance" -> "insurance";
            case "investment" -> "investment";
            default -> "checking";
        };
    }

    /**
     * Mappe les catégories de transaction Budget Insight
     */
    private String mapTransactionCategory(String biCategory) {
        if (biCategory == null) return null;

        return switch (biCategory.toLowerCase()) {
            case "food" -> "alimentation";
            case "transport" -> "transport";
            case "shopping" -> "shopping";
            case "health" -> "sante";
            case "housing" -> "logement";
            case "entertainment" -> "loisirs";
            case "income" -> "salaire";
            default -> biCategory;
        };
    }

    /**
     * Masque les données sensibles pour les logs
     */
    private String maskSensitiveData(String data) {
        if (data == null || data.length() <= 4) {
            return "****";
        }
        return data.substring(0, 2) + "****" + data.substring(data.length() - 2);
    }

    /**
     * Classe interne pour le cache des tokens
     */
    private static class TokenInfo {
        private final String token;
        private final long expiryTime;

        public TokenInfo(String token, long expiryTime) {
            this.token = token;
            this.expiryTime = expiryTime;
        }

        public String getToken() {
            return token;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() >= expiryTime;
        }
    }
}