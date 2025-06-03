// com/master/mosaique_capital/service/banking/BankAccountService.java
package com.master.mosaique_capital.service.banking;

import com.master.mosaique_capital.dto.banking.AccountSummaryDto;
import com.master.mosaique_capital.dto.banking.BankAccountDto;
import com.master.mosaique_capital.entity.BankAccount;
import com.master.mosaique_capital.entity.BankConnection;
import com.master.mosaique_capital.entity.User;
import com.master.mosaique_capital.exception.ResourceNotFoundException;
import com.master.mosaique_capital.mapper.BankAccountMapper;
import com.master.mosaique_capital.repository.BankAccountRepository;
import com.master.mosaique_capital.repository.BankConnectionRepository;
import com.master.mosaique_capital.repository.UserRepository;
import com.master.mosaique_capital.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service de gestion des comptes bancaires
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BankAccountService {

    private final BankAccountRepository bankAccountRepository;
    private final BankConnectionRepository bankConnectionRepository;
    private final UserRepository userRepository;
    private final BankAccountMapper bankAccountMapper;

    /**
     * Récupère tous les comptes bancaires de l'utilisateur connecté
     */
    @Transactional(readOnly = true)
    public List<BankAccountDto> getAllAccounts() {
        User currentUser = getCurrentUser();
        List<BankAccount> accounts = bankAccountRepository.findByConnectionUserOrderByNameAsc(currentUser);
        return bankAccountMapper.toDtoList(accounts);
    }

    /**
     * Récupère un compte bancaire par ID avec vérification d'ownership
     */
    @Transactional(readOnly = true)
    public BankAccountDto getAccountById(Long id) {
        BankAccount account = findAccountById(id);
        checkAccountOwnership(account);
        return bankAccountMapper.toDto(account);
    }

    /**
     * Récupère les comptes d'une connexion bancaire spécifique
     */
    @Transactional(readOnly = true)
    public List<BankAccountDto> getAccountsByConnection(Long connectionId) {
        BankConnection connection = findConnectionById(connectionId);
        checkConnectionOwnership(connection);

        List<BankAccount> accounts = bankAccountRepository.findByConnectionOrderByNameAsc(connection);
        return bankAccountMapper.toDtoList(accounts);
    }

    /**
     * Génère un résumé financier global
     */
    @Transactional(readOnly = true)
    public AccountSummaryDto getAccountsSummary() {
        User currentUser = getCurrentUser();
        List<BankAccount> accounts = bankAccountRepository.findByConnectionUserOrderByNameAsc(currentUser);

        BigDecimal totalBalance = accounts.stream()
                .map(BankAccount::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalAssets = accounts.stream()
                .filter(account -> account.getBalance().compareTo(BigDecimal.ZERO) > 0)
                .map(BankAccount::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalLiabilities = accounts.stream()
                .filter(account -> account.getBalance().compareTo(BigDecimal.ZERO) < 0)
                .map(BankAccount::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .abs();

        Map<String, BigDecimal> balanceByType = accounts.stream()
                .collect(Collectors.groupingBy(
                        BankAccount::getType,
                        Collectors.reducing(BigDecimal.ZERO, BankAccount::getBalance, BigDecimal::add)
                ));

        Map<String, Integer> accountCountByType = accounts.stream()
                .collect(Collectors.groupingBy(
                        BankAccount::getType,
                        Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
                ));

        // TODO: Calculer les revenus/dépenses mensuels depuis les transactions
        BigDecimal monthlyIncome = BigDecimal.ZERO;
        BigDecimal monthlyExpenses = BigDecimal.ZERO;

        return AccountSummaryDto.builder()
                .totalBalance(totalBalance)
                .totalAssets(totalAssets)
                .totalLiabilities(totalLiabilities)
                .totalAccounts(accounts.size())
                .balanceByType(balanceByType)
                .accountCountByType(accountCountByType)
                .monthlyIncome(monthlyIncome)
                .monthlyExpenses(monthlyExpenses)
                .netWorth(totalAssets.subtract(totalLiabilities))
                .build();
    }

    // ===== Méthodes utilitaires =====

    private BankAccount findAccountById(Long id) {
        return bankAccountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Compte bancaire non trouvé avec l'ID: " + id));
    }

    private BankConnection findConnectionById(Long id) {
        return bankConnectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Connexion bancaire non trouvée avec l'ID: " + id));
    }

    private User getCurrentUser() {
        String username = SecurityUtils.getCurrentUsername();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
    }

    private void checkAccountOwnership(BankAccount account) {
        User currentUser = getCurrentUser();
        if (!account.getConnection().getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Vous n'avez pas les droits pour accéder à ce compte bancaire");
        }
    }

    private void checkConnectionOwnership(BankConnection connection) {
        User currentUser = getCurrentUser();
        if (!connection.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Vous n'avez pas les droits pour accéder à cette connexion bancaire");
        }
    }
}