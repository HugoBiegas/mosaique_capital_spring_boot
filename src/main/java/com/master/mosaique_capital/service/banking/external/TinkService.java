// src/main/java/com/master/mosaique_capital/service/banking/external/TinkService.java
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
 * Service d'intégration avec Tink (ex-Nordigen)
 * API Documentation: https://docs.tink.com/api
 * GRATUIT : 100 connexions/mois - Parfait pour démos !
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TinkService {

    private final RestTemplate restTemplate;

    // Cache pour les tokens d'accès Tink
    private final Map<String, TokenInfo> tokenCache = new ConcurrentHashMap<>();

    @Value("${app.banking.tink.api-url:https://ob.nordigen.com/api/v2}")
    private String apiUrl;

    @Value("${app.banking.tink.secret-id:}")
    private String secretId;

    @Value("${app.banking.tink.secret-key:}")
    private String secretKey;

    @Value("${app.banking.tink.enabled:false}")
    private boolean enabled;

    @Value("${app.banking.tink.redirect-url:http://localhost:9999/api/banking/webhooks/tink/callback}")
    private String redirectUrl;

    /**
     * 🚀 Initie une connexion bancaire via Tink - GRATUIT !
     */
    public String initiateConnection(BankConnectionRequest.BankCredentials credentials) {
        if (!enabled) {
            throw new BankConnectionException("Tink n'est pas activé");
        }

        log.info("🔄 Initiation connexion Tink GRATUITE pour institution: {}",
                maskSensitiveData(credentials.getLogin()));

        try {
            // 1. Obtenir un token d'accès
            String accessToken = getOrRefreshAccessToken();

            // 2. Détecter l'institution bancaire depuis le login
            String institutionId = detectBankInstitution(accessToken, credentials.getLogin());

            // 3. Créer un agreement (accord utilisateur)
            String agreementId = createEndUserAgreement(accessToken, institutionId);

            // 4. Créer une requisition (demande de connexion)
            String requisitionId = createRequisition(accessToken, institutionId, agreementId);

            log.info("✅ Connexion Tink initiée GRATUITEMENT. Requisition ID: {}", requisitionId);
            return "tink_" + requisitionId + "_" + agreementId;

        } catch (Exception e) {
            log.error("❌ Erreur lors de l'initiation Tink: {}", e.getMessage(), e);
            throw new BankConnectionException("Échec de la connexion Tink: " + e.getMessage());
        }
    }

    /**
     * 🔐 Confirme une connexion après redirection utilisateur
     */
    public boolean confirmConnection(String connectionId, String confirmationCode) {
        if (!enabled) {
            return false;
        }

        // Parse: tink_<requisitionId>_<agreementId>
        String[] parts = connectionId.replace("tink_", "").split("_", 2);
        if (parts.length != 2) {
            log.error("Format de connectionId Tink invalide: {}", connectionId);
            return false;
        }

        String requisitionId = parts[0];
        String agreementId = parts[1];

        log.info("🔐 Confirmation connexion Tink: {} (agreement: {})", requisitionId, agreementId);

        try {
            String accessToken = getOrRefreshAccessToken();

            // Vérifier le statut de la requisition
            HttpHeaders headers = createAuthHeaders(accessToken);
            HttpEntity<Void> request = new HttpEntity<>(headers);

            String url = String.format("%s/requisitions/%s/", apiUrl, requisitionId);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String status = (String) response.getBody().get("status");

                boolean isLinked = "LN".equals(status); // LN = Linked
                log.info("✅ Confirmation Tink: {} - Statut: {}",
                        isLinked ? "succès" : "en attente", status);

                return isLinked;
            }

            return false;

        } catch (Exception e) {
            log.error("❌ Erreur lors de la confirmation Tink: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 📋 Récupère les comptes d'une connexion Tink
     */
    public List<ExternalAccountDto> getAccounts(String connectionId) {
        if (!enabled) {
            return Collections.emptyList();
        }

        String[] parts = connectionId.replace("tink_", "").split("_", 2);
        if (parts.length != 2) {
            log.error("Format de connectionId Tink invalide pour getAccounts: {}", connectionId);
            return Collections.emptyList();
        }

        String requisitionId = parts[0];

        log.info("📋 Récupération comptes Tink pour requisition: {}", requisitionId);

        try {
            String accessToken = getOrRefreshAccessToken();

            // 1. Récupérer les IDs des comptes depuis la requisition
            List<String> accountIds = getAccountIdsFromRequisition(accessToken, requisitionId);

            if (accountIds.isEmpty()) {
                log.warn("Aucun compte trouvé pour la requisition Tink: {}", requisitionId);
                return Collections.emptyList();
            }

            // 2. Récupérer les détails de chaque compte
            List<ExternalAccountDto> accounts = new ArrayList<>();

            for (String accountId : accountIds) {
                try {
                    ExternalAccountDto account = getAccountDetails(accessToken, accountId);
                    if (account != null) {
                        accounts.add(account);
                    }
                } catch (Exception e) {
                    log.warn("Erreur lors de la récupération du compte {}: {}", accountId, e.getMessage());
                }
            }

            log.info("✅ {} comptes Tink récupérés avec succès", accounts.size());
            return accounts;

        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération des comptes Tink: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 💰 Récupère les transactions d'un compte Tink
     */
    public List<ExternalTransactionDto> getTransactions(String connectionId, String accountId, int days) {
        if (!enabled) {
            return Collections.emptyList();
        }

        log.info("💰 Récupération transactions Tink - Compte: {} (derniers {} jours)",
                accountId, days);

        try {
            String accessToken = getOrRefreshAccessToken();

            HttpHeaders headers = createAuthHeaders(accessToken);
            HttpEntity<Void> request = new HttpEntity<>(headers);

            // Calcul des dates
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(days);

            String url = String.format(
                    "%s/accounts/%s/transactions/?date_from=%s&date_to=%s",
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
                Map<String, Object> data = response.getBody();

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> bookedTransactions =
                        (List<Map<String, Object>>) data.get("transactions");

                if (bookedTransactions == null) {
                    log.info("Aucune transaction trouvée pour le compte Tink: {}", accountId);
                    return Collections.emptyList();
                }

                List<ExternalTransactionDto> transactions = bookedTransactions.stream()
                        .map(this::mapToTinkTransaction)
                        .filter(Objects::nonNull)
                        .toList();

                log.info("✅ {} transactions Tink récupérées pour le compte: {}",
                        transactions.size(), accountId);
                return transactions;
            }

            return Collections.emptyList();

        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération des transactions Tink: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 🏥 Vérifie l'état de santé d'une connexion Tink
     */
    public boolean checkHealth(String connectionId) {
        if (!enabled) {
            return false;
        }

        try {
            String[] parts = connectionId.replace("tink_", "").split("_", 2);
            if (parts.length != 2) {
                return false;
            }

            String requisitionId = parts[0];
            String accessToken = getOrRefreshAccessToken();

            HttpHeaders headers = createAuthHeaders(accessToken);
            HttpEntity<Void> request = new HttpEntity<>(headers);

            String url = String.format("%s/requisitions/%s/", apiUrl, requisitionId);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String status = (String) response.getBody().get("status");
                boolean isHealthy = "LN".equals(status); // LN = Linked

                log.debug("🏥 Santé connexion Tink {}: {} (statut: {})",
                        connectionId, isHealthy ? "OK" : "KO", status);
                return isHealthy;
            }

            return false;

        } catch (Exception e) {
            log.warn("⚠️ Erreur lors de la vérification de santé Tink: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 🗑️ Révoque une connexion Tink
     */
    public boolean revokeConnection(String connectionId) {
        if (!enabled) {
            return false;
        }

        try {
            String[] parts = connectionId.replace("tink_", "").split("_", 2);
            if (parts.length != 2) {
                return false;
            }

            String requisitionId = parts[0];
            String agreementId = parts[1];

            String accessToken = getOrRefreshAccessToken();
            HttpHeaders headers = createAuthHeaders(accessToken);
            HttpEntity<Void> request = new HttpEntity<>(headers);

            // Supprimer la requisition
            String requisitionUrl = String.format("%s/requisitions/%s/", apiUrl, requisitionId);
            restTemplate.exchange(requisitionUrl, HttpMethod.DELETE, request, Void.class);

            // Supprimer l'agreement
            String agreementUrl = String.format("%s/agreements/enduser/%s/", apiUrl, agreementId);
            restTemplate.exchange(agreementUrl, HttpMethod.DELETE, request, Void.class);

            log.info("🗑️ Révocation connexion Tink {} : OK", connectionId);
            return true;

        } catch (Exception e) {
            log.error("❌ Erreur lors de la révocation Tink: {}", e.getMessage(), e);
            return false;
        }
    }

    // ===== Méthodes utilitaires privées =====

    /**
     * 🔑 Obtient ou rafraîchit le token d'accès Tink
     */
    private String getOrRefreshAccessToken() {
        try {
            // Vérifier le cache
            TokenInfo cachedToken = tokenCache.get("tink_access_token");
            if (cachedToken != null && !cachedToken.isExpired()) {
                return cachedToken.token();
            }

            // Obtenir un nouveau token
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> authData = new HashMap<>();
            authData.put("secret_id", secretId);
            authData.put("secret_key", secretKey);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(authData, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    apiUrl + "/token/new/",
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String accessToken = (String) response.getBody().get("access");
                Integer expiresIn = (Integer) response.getBody().get("access_expires");

                // Cache avec marge de sécurité
                long expiryTime = System.currentTimeMillis() + (expiresIn * 900L); // 90%
                tokenCache.put("tink_access_token", new TokenInfo(accessToken, expiryTime));

                log.debug("🔑 Nouveau token Tink obtenu GRATUITEMENT, expire dans {}s", expiresIn);
                return accessToken;
            }

            throw new BankConnectionException("Impossible d'obtenir le token Tink");

        } catch (Exception e) {
            log.error("❌ Erreur lors de l'obtention du token Tink: {}", e.getMessage(), e);
            throw new BankConnectionException("Échec de l'authentification Tink: " + e.getMessage());
        }
    }

    /**
     * 🏦 Détecte l'institution bancaire depuis le login
     */
    private String detectBankInstitution(String accessToken, String login) {
        HttpHeaders headers = createAuthHeaders(accessToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        // Récupérer les institutions françaises
        ResponseEntity<Map> response = restTemplate.exchange(
                apiUrl + "/institutions/?country=FR",
                HttpMethod.GET,
                request,
                Map.class
        );

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> institutions =
                    (List<Map<String, Object>>) response.getBody().get("results");

            if (institutions == null || institutions.isEmpty()) {
                throw new BankConnectionException("Aucune institution Tink disponible en France");
            }

            // Détection intelligente par nom
            String loginLower = login.toLowerCase();

            for (Map<String, Object> institution : institutions) {
                String name = (String) institution.get("name");
                if (name == null) continue;

                String nameLower = name.toLowerCase();

                // Correspondances françaises principales
                if (loginLower.contains("creditagricole") && nameLower.contains("crédit agricole")) {
                    return institution.get("id").toString();
                }
                if (loginLower.contains("bnpparibas") && nameLower.contains("bnp")) {
                    return institution.get("id").toString();
                }
                if (loginLower.contains("societegenerale") && nameLower.contains("société générale")) {
                    return institution.get("id").toString();
                }
                if (loginLower.contains("lcl") && nameLower.contains("lcl")) {
                    return institution.get("id").toString();
                }
            }

            // Fallback sur la première institution française disponible
            String institutionId = institutions.get(0).get("id").toString();
            String institutionName = (String) institutions.get(0).get("name");

            log.info("🏦 Utilisation de l'institution par défaut: {} ({})",
                    institutionName, institutionId);
            return institutionId;
        }

        throw new BankConnectionException("Impossible de récupérer les institutions Tink");
    }

    /**
     * 📄 Crée un agreement (accord utilisateur)
     */
    private String createEndUserAgreement(String accessToken, String institutionId) {
        HttpHeaders headers = createAuthHeaders(accessToken);

        Map<String, Object> agreementData = new HashMap<>();
        agreementData.put("institution_id", institutionId);
        agreementData.put("max_historical_days", 90); // 3 mois d'historique
        agreementData.put("access_valid_for_days", 90); // Valide 3 mois
        agreementData.put("access_scope", Arrays.asList("balances", "details", "transactions"));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(agreementData, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                apiUrl + "/agreements/enduser/",
                HttpMethod.POST,
                request,
                Map.class
        );

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            String agreementId = (String) response.getBody().get("id");
            log.debug("📄 Agreement Tink créé: {}", agreementId);
            return agreementId;
        }

        throw new BankConnectionException("Impossible de créer l'agreement Tink");
    }

    /**
     * 📝 Crée une requisition (demande de connexion)
     */
    private String createRequisition(String accessToken, String institutionId, String agreementId) {
        HttpHeaders headers = createAuthHeaders(accessToken);

        Map<String, Object> requisitionData = new HashMap<>();
        requisitionData.put("redirect", redirectUrl);
        requisitionData.put("institution_id", institutionId);
        requisitionData.put("agreement", agreementId);
        requisitionData.put("reference", "mosaique_" + System.currentTimeMillis());
        requisitionData.put("user_language", "FR");

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requisitionData, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                apiUrl + "/requisitions/",
                HttpMethod.POST,
                request,
                Map.class
        );

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            String requisitionId = (String) response.getBody().get("id");
            log.debug("📝 Requisition Tink créée: {}", requisitionId);
            return requisitionId;
        }

        throw new BankConnectionException("Impossible de créer la requisition Tink");
    }

    /**
     * 🆔 Récupère les IDs des comptes depuis une requisition
     */
    private List<String> getAccountIdsFromRequisition(String accessToken, String requisitionId) {
        HttpHeaders headers = createAuthHeaders(accessToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        String url = String.format("%s/requisitions/%s/", apiUrl, requisitionId);

        ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                request,
                Map.class
        );

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            @SuppressWarnings("unchecked")
            List<String> accounts = (List<String>) response.getBody().get("accounts");
            return accounts != null ? accounts : Collections.emptyList();
        }

        return Collections.emptyList();
    }

    /**
     * 💳 Récupère les détails d'un compte
     */
    private ExternalAccountDto getAccountDetails(String accessToken, String accountId) {
        HttpHeaders headers = createAuthHeaders(accessToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        // Récupérer les détails du compte
        String detailsUrl = String.format("%s/accounts/%s/details/", apiUrl, accountId);
        ResponseEntity<Map> detailsResponse = restTemplate.exchange(
                detailsUrl, HttpMethod.GET, request, Map.class);

        // Récupérer les soldes
        String balancesUrl = String.format("%s/accounts/%s/balances/", apiUrl, accountId);
        ResponseEntity<Map> balancesResponse = restTemplate.exchange(
                balancesUrl, HttpMethod.GET, request, Map.class);

        if (detailsResponse.getStatusCode().is2xxSuccessful() &&
                balancesResponse.getStatusCode().is2xxSuccessful() &&
                detailsResponse.getBody() != null &&
                balancesResponse.getBody() != null) {

            return mapToTinkAccount(detailsResponse.getBody(), balancesResponse.getBody(), accountId);
        }

        return null;
    }

    /**
     * 🔧 Crée les headers d'authentification Tink
     */
    private HttpHeaders createAuthHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        return headers;
    }

    /**
     * 💳 Mappe un compte Tink vers notre DTO
     */
    private ExternalAccountDto mapToTinkAccount(Map<String, Object> details,
                                                Map<String, Object> balances,
                                                String accountId) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> account = (Map<String, Object>) details.get("account");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> balanceList =
                    (List<Map<String, Object>>) balances.get("balances");

            String accountName = (String) account.get("name");
            String iban = (String) account.get("iban");
            String currency = (String) account.get("currency");

            // Récupérer le solde disponible
            BigDecimal balance = BigDecimal.ZERO;
            if (balanceList != null && !balanceList.isEmpty()) {
                for (Map<String, Object> bal : balanceList) {
                    if ("expected".equals(bal.get("balanceType"))) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> balanceAmount =
                                (Map<String, Object>) bal.get("balanceAmount");
                        if (balanceAmount != null) {
                            balance = new BigDecimal(balanceAmount.get("amount").toString());
                            break;
                        }
                    }
                }
            }

            return ExternalAccountDto.builder()
                    .externalId(accountId)
                    .name(accountName != null ? accountName : "Compte Tink")
                    .type("checking") // Tink ne spécifie pas toujours le type
                    .balance(balance)
                    .currency(currency != null ? currency : "EUR")
                    .iban(iban)
                    .status("ACTIVE")
                    .build();

        } catch (Exception e) {
            log.error("❌ Erreur lors du mapping du compte Tink: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 💰 Mappe une transaction Tink vers notre DTO
     */
    private ExternalTransactionDto mapToTinkTransaction(Map<String, Object> transactionData) {
        try {
            String transactionId = (String) transactionData.get("transactionId");

            @SuppressWarnings("unchecked")
            Map<String, Object> transactionAmount =
                    (Map<String, Object>) transactionData.get("transactionAmount");

            BigDecimal amount = BigDecimal.ZERO;
            if (transactionAmount != null) {
                amount = new BigDecimal(transactionAmount.get("amount").toString());
            }

            String bookingDate = (String) transactionData.get("bookingDate");
            String valueDate = (String) transactionData.get("valueDate");

            LocalDate transactionDate = bookingDate != null ?
                    LocalDate.parse(bookingDate) : LocalDate.now();
            LocalDate valDate = valueDate != null ?
                    LocalDate.parse(valueDate) : transactionDate;

            String remittanceInfo = (String) transactionData.get("remittanceInformationUnstructured");

            return ExternalTransactionDto.builder()
                    .externalId(transactionId)
                    .amount(amount)
                    .description(remittanceInfo != null ? remittanceInfo : "Transaction Tink")
                    .transactionDate(transactionDate)
                    .valueDate(valDate)
                    .category(null) // Tink ne fournit pas de catégorie automatique
                    .type(amount.compareTo(BigDecimal.ZERO) >= 0 ? "CREDIT" : "DEBIT")
                    .build();

        } catch (Exception e) {
            log.error("❌ Erreur lors du mapping de la transaction Tink: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 🔒 Masque les données sensibles
     */
    private String maskSensitiveData(String data) {
        if (data == null || data.length() <= 4) {
            return "****";
        }
        return data.substring(0, 2) + "****" + data.substring(data.length() - 2);
    }

    /**
     * 🏷️ Classe interne pour le cache des tokens
     */
    private record TokenInfo(@Getter String token, long expiryTime) {
        public boolean isExpired() {
            return System.currentTimeMillis() >= expiryTime;
        }
    }
}