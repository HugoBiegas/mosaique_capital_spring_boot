// com/master/mosaique_capital/service/banking/TransactionCategorizationService.java
package com.master.mosaique_capital.service.banking;

import com.master.mosaique_capital.entity.BankTransaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Service de catégorisation automatique des transactions bancaires
 */
@Service
@Slf4j
public class TransactionCategorizationService {

    // Patterns de catégorisation basés sur la description
    private static final Map<String, Pattern> CATEGORY_PATTERNS = Map.of(
            "alimentation", Pattern.compile("(?i).*(monoprix|carrefour|franprix|leclerc|auchan|intermarche|super u|casino|lidl|metro|picard).*"),
            "transport", Pattern.compile("(?i).*(sncf|ratp|uber|taxi|essence|station|total|shell|bp|esso|autoroute|parking).*"),
            "restaurant", Pattern.compile("(?i).*(restaurant|bistro|brasserie|cafe|mcdo|kfc|burger|pizza|deliveroo|uber eats).*"),
            "shopping", Pattern.compile("(?i).*(amazon|cdiscount|fnac|darty|h&m|zara|uniqlo|decathlon|ikea).*"),
            "sante", Pattern.compile("(?i).*(pharmacie|medecin|dentiste|hopital|clinique|mutuelle|secu|cpam).*"),
            "logement", Pattern.compile("(?i).*(loyer|charges|edf|gdf|eau|internet|orange|sfr|free|bouygues|assurance|habitation).*"),
            "salaire", Pattern.compile("(?i).*(salaire|paie|remuneration|traitement|entreprise).*"),
            "banque", Pattern.compile("(?i).*(virement|commission|frais|cotisation|carte|retrait|dab).*"),
            "loisirs", Pattern.compile("(?i).*(cinema|theatre|concert|sport|abonnement|netflix|spotify|deezer|canal).*"),
            "education", Pattern.compile("(?i).*(ecole|universite|formation|cours|livre|etudiant).*")
    );

    /**
     * Catégorise automatiquement une transaction basée sur sa description
     */
    public String categorizeTransaction(BankTransaction transaction) {
        if (transaction.getDescription() == null || transaction.getDescription().trim().isEmpty()) {
            return null;
        }

        String description = transaction.getDescription().toLowerCase().trim();

        // Parcourt les patterns pour trouver une correspondance
        for (Map.Entry<String, Pattern> entry : CATEGORY_PATTERNS.entrySet()) {
            if (entry.getValue().matcher(description).matches()) {
                log.debug("Transaction '{}' catégorisée comme: {}", description, entry.getKey());
                return entry.getKey();
            }
        }

        // Catégorisation basée sur le montant pour les virements
        if (description.contains("virement")) {
            if (transaction.getAmount().compareTo(java.math.BigDecimal.ZERO) > 0) {
                return "virement_entrant";
            } else {
                return "virement_sortant";
            }
        }

        // Catégorie par défaut si aucun pattern ne correspond
        log.debug("Aucune catégorie trouvée pour la transaction: {}", description);
        return "autres";
    }

    /**
     * Recatégorise toutes les transactions d'un utilisateur
     */
    public void recategorizeAllTransactions(java.util.List<BankTransaction> transactions) {
        log.info("Début de la recatégorisation de {} transactions", transactions.size());

        int categorized = 0;
        for (BankTransaction transaction : transactions) {
            String newCategory = categorizeTransaction(transaction);
            if (newCategory != null && !newCategory.equals(transaction.getCategory())) {
                transaction.setCategory(newCategory);
                categorized++;
            }
        }

        log.info("Recatégorisation terminée: {} transactions mises à jour", categorized);
    }

    /**
     * Ajoute un nouveau pattern de catégorisation
     */
    public void addCategoryPattern(String category, String pattern) {
        // Cette méthode pourrait être étendue pour permettre l'ajout dynamique de patterns
        // Pour l'instant, les patterns sont statiques
        log.info("Demande d'ajout de pattern pour la catégorie '{}': {}", category, pattern);
    }
}