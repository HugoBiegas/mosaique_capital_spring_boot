// com/master/mosaique_capital/service/banking/BankTransactionService.java
package com.master.mosaique_capital.service.banking;

import com.master.mosaique_capital.dto.banking.BankTransactionDto;
import com.master.mosaique_capital.dto.banking.TransactionSearchCriteria;
import com.master.mosaique_capital.entity.BankTransaction;
import com.master.mosaique_capital.entity.User;
import com.master.mosaique_capital.exception.ResourceNotFoundException;
import com.master.mosaique_capital.mapper.BankTransactionMapper;
import com.master.mosaique_capital.repository.BankTransactionRepository;
import com.master.mosaique_capital.repository.UserRepository;
import com.master.mosaique_capital.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service de gestion des transactions bancaires
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BankTransactionService {

    private final BankTransactionRepository bankTransactionRepository;
    private final UserRepository userRepository;
    private final BankTransactionMapper bankTransactionMapper;

    /**
     * Recherche des transactions avec critères dynamiques
     */
    @Transactional(readOnly = true)
    public Page<BankTransactionDto> searchTransactions(TransactionSearchCriteria criteria) {
        User currentUser = getCurrentUser();

        // Construction de la spécification dynamique
        Specification<BankTransaction> spec = createTransactionSpecification(currentUser, criteria);

        // Configuration de la pagination et du tri
        Sort sort = Sort.by(
                "DESC".equalsIgnoreCase(criteria.getSortDirection()) ? Sort.Direction.DESC : Sort.Direction.ASC,
                criteria.getSortBy()
        );

        Pageable pageable = PageRequest.of(criteria.getPage(), criteria.getSize(), sort);

        Page<BankTransaction> transactions = bankTransactionRepository.findAll(spec, pageable);
        return transactions.map(bankTransactionMapper::toDto);
    }

    /**
     * Récupère une transaction par ID avec vérification d'ownership
     */
    @Transactional(readOnly = true)
    public BankTransactionDto getTransactionById(Long id) {
        BankTransaction transaction = findTransactionById(id);
        checkTransactionOwnership(transaction);
        return bankTransactionMapper.toDto(transaction);
    }

    /**
     * Met à jour la catégorie d'une transaction
     */
    @Transactional
    public BankTransactionDto updateTransactionCategory(Long id, String category) {
        BankTransaction transaction = findTransactionById(id);
        checkTransactionOwnership(transaction);

        log.info("Mise à jour de la catégorie de la transaction ID: {} vers '{}'", id, category);

        transaction.setCategory(category);
        BankTransaction savedTransaction = bankTransactionRepository.save(transaction);

        return bankTransactionMapper.toDto(savedTransaction);
    }

    /**
     * Récupère les statistiques de dépenses par catégorie
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getCategoryStatistics(LocalDate startDate, LocalDate endDate) {
        User currentUser = getCurrentUser();

        // Dates par défaut si non spécifiées (dernier mois)
        if (startDate == null) {
            startDate = LocalDate.now().minusMonths(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        List<BankTransactionRepository.CategoryExpenseProjection> expenses =
                bankTransactionRepository.getExpensesByCategory(currentUser, startDate, endDate);

        Map<String, BigDecimal> categoryExpenses = new HashMap<>();
        BigDecimal totalExpenses = BigDecimal.ZERO;

        for (BankTransactionRepository.CategoryExpenseProjection expense : expenses) {
            String category = expense.getCategory() != null ? expense.getCategory() : "non_categorise";
            categoryExpenses.put(category, expense.getTotal());
            totalExpenses = totalExpenses.add(expense.getTotal());
        }

        Map<String, Object> statistics = new HashMap<>();
        statistics.put("period", Map.of("startDate", startDate, "endDate", endDate));
        statistics.put("categoryExpenses", categoryExpenses);
        statistics.put("totalExpenses", totalExpenses);
        statistics.put("averageDailyExpense",
                totalExpenses.divide(BigDecimal.valueOf(startDate.until(endDate).getDays() + 1), 2, java.math.RoundingMode.HALF_UP));

        return statistics;
    }

    /**
     * Récupère les statistiques de revenus vs dépenses
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getCashFlowStatistics(LocalDate startDate, LocalDate endDate) {
        User currentUser = getCurrentUser();

        // Dates par défaut si non spécifiées (dernier mois)
        if (startDate == null) {
            startDate = LocalDate.now().minusMonths(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        BigDecimal totalIncome = bankTransactionRepository.getTotalIncomeByUserAndDateRange(currentUser, startDate, endDate);
        BigDecimal totalExpenses = bankTransactionRepository.getTotalExpensesByUserAndDateRange(currentUser, startDate, endDate);

        if (totalIncome == null) totalIncome = BigDecimal.ZERO;
        if (totalExpenses == null) totalExpenses = BigDecimal.ZERO;

        BigDecimal netCashFlow = totalIncome.subtract(totalExpenses);

        Map<String, Object> statistics = new HashMap<>();
        statistics.put("period", Map.of("startDate", startDate, "endDate", endDate));
        statistics.put("totalIncome", totalIncome);
        statistics.put("totalExpenses", totalExpenses);
        statistics.put("netCashFlow", netCashFlow);
        statistics.put("savingsRate",
                totalIncome.compareTo(BigDecimal.ZERO) > 0 ?
                        netCashFlow.divide(totalIncome, 4, java.math.RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)) :
                        BigDecimal.ZERO);

        return statistics;
    }

    /**
     * Récupère le nombre de transactions non catégorisées
     */
    @Transactional(readOnly = true)
    public long getUncategorizedTransactionsCount() {
        User currentUser = getCurrentUser();
        return bankTransactionRepository.countUncategorizedTransactionsByUser(currentUser);
    }

    // ===== Méthodes utilitaires =====

    private Specification<BankTransaction> createTransactionSpecification(User user, TransactionSearchCriteria criteria) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filtre sur l'utilisateur (sécurité)
            predicates.add(criteriaBuilder.equal(root.get("account").get("connection").get("user"), user));

            // Filtre par compte si spécifié
            if (criteria.getAccountId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("account").get("id"), criteria.getAccountId()));
            }

            // Filtre par période
            if (criteria.getStartDate() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("transactionDate"), criteria.getStartDate()));
            }
            if (criteria.getEndDate() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("transactionDate"), criteria.getEndDate()));
            }

            // Filtre par montant
            if (criteria.getMinAmount() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("amount"), criteria.getMinAmount()));
            }
            if (criteria.getMaxAmount() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("amount"), criteria.getMaxAmount()));
            }

            // Filtre par catégorie
            if (criteria.getCategory() != null && !criteria.getCategory().trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("category"), criteria.getCategory()));
            }

            // Filtre par type (DEBIT/CREDIT)
            if (criteria.getType() != null && !criteria.getType().trim().isEmpty()) {
                if ("DEBIT".equalsIgnoreCase(criteria.getType())) {
                    predicates.add(criteriaBuilder.lessThan(root.get("amount"), BigDecimal.ZERO));
                } else if ("CREDIT".equalsIgnoreCase(criteria.getType())) {
                    predicates.add(criteriaBuilder.greaterThan(root.get("amount"), BigDecimal.ZERO));
                }
            }

            // Filtre par description
            if (criteria.getDescription() != null && !criteria.getDescription().trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("description")),
                        "%" + criteria.getDescription().toLowerCase() + "%"
                ));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private BankTransaction findTransactionById(Long id) {
        return bankTransactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction non trouvée avec l'ID: " + id));
    }

    private User getCurrentUser() {
        String username = SecurityUtils.getCurrentUsername();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
    }

    private void checkTransactionOwnership(BankTransaction transaction) {
        User currentUser = getCurrentUser();
        if (!transaction.getAccount().getConnection().getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Vous n'avez pas les droits pour accéder à cette transaction");
        }
    }
}