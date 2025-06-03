// src/main/java/com/master/mosaique_capital/service/banking/external/BridgeApiService.java
package com.master.mosaique_capital.service.banking.external;

import com.master.mosaique_capital.dto.banking.BankConnectionRequest;
import com.master.mosaique_capital.dto.banking.external.ExternalAccountDto;
import com.master.mosaique_capital.dto.banking.external.ExternalTransactionDto;
import com.master.mosaique_capital.exception.BankConnectionException;
import lombok.Getter;
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
 * Service d'intégration avec Bridge API (bridge-api.io)
 * Documentation: https://docs.bridgeapi.io/
 * Approche moderne avec excellentes performances et routing intelligent
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BridgeApiService {

    private final RestTemplate restTemplate;

    // Cache pour les tokens d'accès Bridge
    private final Map<String, TokenInfo> tokenCache = new ConcurrentHashMap<>();

    // ✅ CORRECTION : Ajout de valeurs par défaut pour éviter les erreurs de démarrage
    @Value("${app.banking.bridge.api-url:https://api.bridgeapi.io/v2}")
    private String apiUrl;

    @Value("${app.banking.bridge.client-id:demo_bridge_client}")
    private String clientId;

    @Value("${app.banking.bridge.client-secret:demo_bridge_secret}")
    private String clientSecret;

    @Value("${app.banking.bridge.enabled:false}")
    private boolean enabled;

    @Value("${app.banking.bridge.sandbox:true}")
    private boolean sandboxMode;

    /**
     * Initie une connexion bancaire via Bridge API
     */
    public String initiateConnection(BankConnectionRequest.BankCredentials credentials) {
        if (!enabled) {
            throw new BankConnectionException("Bridge API n'est pas activé");
        }

        log.info("🌉 Initiation connexion Bridge API pour l'utilisateur: {}",
                maskSensitiveData(credentials.getLogin()));

        try {
            // 1. Obtenir un token d'accès
            String accessToken = getOrRefreshAccessToken();

            // 2. Créer un utilisateur Bridge
            String userId = createBridgeUser(accessToken, credentials);

            // 3. Détecter la banque via l'API Bridge
            String bankId = detectBankFromCredentials(accessToken, credentials);

            // 4. Initier la connexion avec authentification
            String itemId = initiateBankConnection(accessToken, userId, bankId, credentials);

            log.info("✅ Connexion Bridge API initiée. Item ID: {}", itemId);
            return "bridge_" + itemId + "_" + userId;

        } catch (Exception e) {
            log.error("❌ Erreur lors de l'initiation Bridge API: {}", e.getMessage(), e);
            throw new BankConnectionException("Échec de la connexion Bridge API: " + e.getMessage());
        }
    }

    /**
     * Confirme une connexion après authentification forte
     */
    public boolean confirmConnection(String connectionId, String confirmationCode) {
        if (!enabled) {
            return false;
        }

        // Parse: bridge_<itemId>_<userId>
        String[] parts = connectionId.replace("bridge_", "").split("_", 2);
        if (parts.length != 2) {
            log.error("Format de connectionId Bridge invalide: {}", connectionId);
            return false;
        }

        String itemId = parts[0];
        String userId = parts[1];

        log.info("🔐 Confirmation connexion Bridge API: {} pour utilisateur: {}", itemId, userId);

        try {
            String accessToken = getOrRefreshAccessToken();

            HttpHeaders headers = createAuthHeaders(accessToken);

            Map<String, Object> challengeData = new HashMap<>();
            if (confirmationCode != null && !confirmationCode.trim().isEmpty()) {
                challengeData.put("challenge_solution", confirmationCode);
            }

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(challengeData, headers);

            String url = String.format("%s/items/%s/mfa", apiUrl, itemId);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String status = (String) response.getBody().get("status");
                boolean isActive = "CONNECTED".equals(status) || "SYNCING".equals(status);

                log.info("✅ Confirmation Bridge API: {} - Statut: {}", isActive ? "succès" : "échec", status);
                return isActive;
            }

            return false;

        } catch (RestClientException e) {
            log.error("❌ Erreur réseau lors de la confirmation Bridge API: {}", e.getMessage(), e);
            return false;
        } catch (Exception e) {
            log.error("❌ Erreur inattendue lors de la confirmation Bridge API: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Récupère les comptes d'une connexion Bridge
     */
    public List<ExternalAccountDto> getAccounts(String connectionId) {
        if (!enabled) {
            return Collections.emptyList();
        }

        String[] parts = connectionId.replace("bridge_", "").split("_", 2);
        if (parts.length != 2) {
            log.error("Format de connectionId Bridge invalide pour getAccounts: {}", connectionId);
            return Collections.emptyList();
        }

        String itemId = parts[0];

        log.info("📋 Récupération comptes Bridge API pour item: {}", itemId);

        try {
            String accessToken = getOrRefreshAccessToken();

            HttpHeaders headers = createAuthHeaders(accessToken);
            HttpEntity<Void> request = new HttpEntity<>(headers);

            String url = String.format("%s/items/%s/accounts", apiUrl, itemId);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> accountsData = (List<Map<String, Object>>)
                        response.getBody().get("resources");

                if (accountsData == null) {
                    log.warn("Aucune donnée de compte trouvée dans la réponse Bridge");
                    return Collections.emptyList();
                }

                List<ExternalAccountDto> accounts = accountsData.stream()
                        .map(this::mapToBridgeAccount)
                        .filter(Objects::nonNull)
                        .toList();

                log.info("✅ {} comptes Bridge récupérés avec succès", accounts.size());
                return accounts;
            }

            return Collections.emptyList();

        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération des comptes Bridge API: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Récupère les transactions d'un compte Bridge
     */
    public List<ExternalTransactionDto> getTransactions(String connectionId, String accountId, int days) {
        if (!enabled) {
            return Collections.emptyList();
        }

        String[] parts = connectionId.replace("bridge_", "").split("_", 2);
        if (parts.length != 2) {
            return Collections.emptyList();
        }

        log.info("💰 Récupération transactions Bridge API - Compte: {} (derniers {} jours)",
                accountId, days);

        try {
            String accessToken = getOrRefreshAccessToken();

            HttpHeaders headers = createAuthHeaders(accessToken);
            HttpEntity<Void> request = new HttpEntity<>(headers);

            // Calcul des dates
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(days);

            String url = String.format(
                    "%s/accounts/%s/transactions?since=%s&until=%s&limit=500",
                    apiUrl, accountId,
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
                        response.getBody().get("resources");

                if (transactionsData == null) {
                    log.info("Aucune transaction trouvée pour le compte Bridge: {}", accountId);
                    return Collections.emptyList();
                }

                List<ExternalTransactionDto> transactions = transactionsData.stream()
                        .map(this::mapToBridgeTransaction)
                        .filter(Objects::nonNull)
                        .toList();

                log.info("✅ {} transactions Bridge récupérées pour le compte: {}",
                        transactions.size(), accountId);
                return transactions;
            }

            return Collections.emptyList();

        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération des transactions Bridge API: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Vérifie l'état de santé d'une connexion Bridge
     */
    public boolean checkHealth(String connectionId) {
        if (!enabled) {
            return false;
        }

        try {
            String[] parts = connectionId.replace("bridge_", "").split("_", 2);
            if (parts.length != 2) {
                return false;
            }

            String itemId = parts[0];
            String accessToken = getOrRefreshAccessToken();

            HttpHeaders headers = createAuthHeaders(accessToken);
            HttpEntity<Void> request = new HttpEntity<>(headers);

            String url = String.format("%s/items/%s", apiUrl, itemId);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String status = (String) response.getBody().get("status");
                boolean isHealthy = "CONNECTED".equals(status);

                log.debug("🏥 Santé connexion Bridge API {}: {} (statut: {})",
                        connectionId, isHealthy ? "OK" : "KO", status);
                return isHealthy;
            }

            return false;

        } catch (Exception e) {
            log.warn("⚠️ Erreur lors de la vérification de santé Bridge API: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Révoque une connexion Bridge
     */
    public boolean revokeConnection(String connectionId) {
        if (!enabled) {
            return false;
        }

        try {
            String[] parts = connectionId.replace("bridge_", "").split("_", 2);
            if (parts.length != 2) {
                return false;
            }

            String itemId = parts[0];
            String accessToken = getOrRefreshAccessToken();

            HttpHeaders headers = createAuthHeaders(accessToken);
            HttpEntity<Void> request = new HttpEntity<>(headers);

            String url = String.format("%s/items/%s", apiUrl, itemId);

            ResponseEntity<Void> response = restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    request,
                    Void.class
            );

            boolean revoked = response.getStatusCode().is2xxSuccessful();
            log.info("🗑️ Révocation connexion Bridge API {}: {}", connectionId, revoked ? "OK" : "KO");

            return revoked;

        } catch (Exception e) {
            log.error("❌ Erreur lors de la révocation Bridge API: {}", e.getMessage(), e);
            return false;
        }
    }

    // ===== Méthodes utilitaires privées =====

    /**
     * Obtient ou rafraîchit le token d'accès Bridge
     */
    private String getOrRefreshAccessToken() {
        try {
            // Vérifier le cache
            TokenInfo cachedToken = tokenCache.get("bridge_access_token");
            if (cachedToken != null && !cachedToken.isExpired()) {
                return cachedToken.token();
            }

            // Obtenir un nouveau token via client credentials
            String credentials = Base64.getEncoder().encodeToString(
                    (clientId + ":" + clientSecret).getBytes()
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Authorization", "Basic " + credentials);

            String body = "grant_type=client_credentials";
            HttpEntity<String> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    apiUrl + "/token",
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String accessToken = (String) response.getBody().get("access_token");
                Integer expiresIn = (Integer) response.getBody().get("expires_in");

                // Cache avec marge de sécurité
                long expiryTime = System.currentTimeMillis() + (expiresIn * 900L); // 90%
                tokenCache.put("bridge_access_token", new TokenInfo(accessToken, expiryTime));

                log.debug("🔑 Nouveau token Bridge API obtenu, expire dans {}s", expiresIn);
                return accessToken;
            }

            throw new BankConnectionException("Impossible d'obtenir le token Bridge API");

        } catch (Exception e) {
            log.error("❌ Erreur lors de l'obtention du token Bridge API: {}", e.getMessage(), e);
            throw new BankConnectionException("Échec de l'authentification Bridge API: " + e.getMessage());
        }
    }

    /**
     * Crée un utilisateur Bridge
     */
    private String createBridgeUser(String accessToken, BankConnectionRequest.BankCredentials credentials) {
        HttpHeaders headers = createAuthHeaders(accessToken);

        Map<String, Object> userData = new HashMap<>();
        userData.put("email", credentials.getLogin()); // Bridge utilise l'email comme identifiant
        userData.put("password", credentials.getPassword());

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(userData, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                apiUrl + "/users",
                HttpMethod.POST,
                request,
                Map.class
        );

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            Object userId = response.getBody().get("uuid");
            log.debug("👤 Utilisateur Bridge créé: {}", userId);
            return userId.toString();
        }

        throw new BankConnectionException("Impossible de créer l'utilisateur Bridge");
    }

    /**
     * Détecte la banque depuis les credentials
     */
    private String detectBankFromCredentials(String accessToken, BankConnectionRequest.BankCredentials credentials) {
        // Bridge API a une approche plus intelligente pour détecter les banques
        // Utilisation de l'endpoint /banks pour récupérer la liste disponible

        HttpHeaders headers = createAuthHeaders(accessToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                apiUrl + "/banks?country=fr",
                HttpMethod.GET,
                request,
                Map.class
        );

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> banks = (List<Map<String, Object>>) response.getBody().get("resources");

            // Logique de détection améliorée pour Bridge
            String loginLower = credentials.getLogin().toLowerCase();

            for (Map<String, Object> bank : banks) {
                String bankName = (String) bank.get("name");
                if (bankName == null) continue;

                String bankNameLower = bankName.toLowerCase();

                // Correspondances spécifiques à Bridge API
                if (loginLower.contains("creditagricole") && bankNameLower.contains("crédit agricole")) {
                    return bank.get("id").toString();
                }
                if (loginLower.contains("bnpparibas") && bankNameLower.contains("bnp")) {
                    return bank.get("id").toString();
                }
            }

            // Fallback sur la première banque française disponible
            return banks.get(0).get("id").toString();
        }

        throw new BankConnectionException("Aucune banque Bridge API disponible");
    }

    /**
     * Initie la connexion bancaire Bridge
     */
    private String initiateBankConnection(String accessToken, String userId, String bankId,
                                          BankConnectionRequest.BankCredentials credentials) {
        HttpHeaders headers = createAuthHeaders(accessToken);

        Map<String, Object> itemData = new HashMap<>();
        itemData.put("bank_id", bankId);
        itemData.put("user_uuid", userId);
        itemData.put("credentials", Map.of(
                "login", credentials.getLogin(),
                "password", credentials.getPassword()
        ));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(itemData, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                apiUrl + "/items",
                HttpMethod.POST,
                request,
                Map.class
        );

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            Object itemId = response.getBody().get("id");
            return itemId.toString();
        }

        throw new BankConnectionException("Impossible de créer la connexion Bridge");
    }

    /**
     * Crée les headers d'authentification Bridge
     */
    private HttpHeaders createAuthHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        headers.set("Bridge-Version", "2021-06-01"); // Version API Bridge
        return headers;
    }

    /**
     * Mappe un compte Bridge vers notre DTO
     */
    private ExternalAccountDto mapToBridgeAccount(Map<String, Object> accountData) {
        try {
            // Bridge retourne les montants en centimes
            Object balanceObj = accountData.get("balance");
            BigDecimal balance = balanceObj != null ?
                    new BigDecimal(balanceObj.toString()).divide(new BigDecimal("100")) :
                    BigDecimal.ZERO;

            return ExternalAccountDto.builder()
                    .externalId(accountData.get("id").toString())
                    .name((String) accountData.get("name"))
                    .type(mapBridgeAccountType((String) accountData.get("type")))
                    .balance(balance)
                    .currency((String) accountData.getOrDefault("currency_code", "EUR"))
                    .iban((String) accountData.get("iban"))
                    .status(accountData.getOrDefault("is_pro", false).equals(false) ? "ACTIVE" : "PROFESSIONAL")
                    .build();
        } catch (Exception e) {
            log.error("❌ Erreur lors du mapping du compte Bridge: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Mappe une transaction Bridge vers notre DTO
     */
    private ExternalTransactionDto mapToBridgeTransaction(Map<String, Object> transactionData) {
        try {
            // Bridge retourne les montants en centimes
            Object amountObj = transactionData.get("amount");
            BigDecimal amount = amountObj != null ?
                    new BigDecimal(amountObj.toString()).divide(new BigDecimal("100")) :
                    BigDecimal.ZERO;

            String dateStr = (String) transactionData.get("date");
            LocalDate transactionDate = dateStr != null ?
                    LocalDate.parse(dateStr) : LocalDate.now();

            return ExternalTransactionDto.builder()
                    .externalId(transactionData.get("id").toString())
                    .amount(amount)
                    .description((String) transactionData.get("description"))
                    .transactionDate(transactionDate)
                    .valueDate(transactionDate) // Bridge n'a qu'une seule date
                    .category(mapBridgeTransactionCategory((String) transactionData.get("category_id")))
                    .type(amount.compareTo(BigDecimal.ZERO) >= 0 ? "CREDIT" : "DEBIT")
                    .build();
        } catch (Exception e) {
            log.error("❌ Erreur lors du mapping de la transaction Bridge: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Mappe les types de compte Bridge
     */
    private String mapBridgeAccountType(String bridgeType) {
        if (bridgeType == null) return "checking";

        return switch (bridgeType.toLowerCase()) {
            case "checking" -> "checking";
            case "savings" -> "savings";
            case "credit_card" -> "card";
            case "loan" -> "loan";
            case "investment" -> "investment";
            default -> "checking";
        };
    }

    /**
     * Mappe les catégories de transaction Bridge
     */
    private String mapBridgeTransactionCategory(String bridgeCategory) {
        if (bridgeCategory == null) return null;

        return switch (bridgeCategory) {
            case "1" -> "alimentation";
            case "2" -> "transport";
            case "3" -> "shopping";
            case "4" -> "sante";
            case "5" -> "logement";
            case "6" -> "loisirs";
            case "7" -> "salaire";
            default -> "autres";
        };
    }

    /**
     * Masque les données sensibles
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
    private record TokenInfo(@Getter String token, long expiryTime) {

        public boolean isExpired() {
            return System.currentTimeMillis() >= expiryTime;
        }
    }
}