// com/master/mosaique_capital/service/banking/BankAccountSyncService.java
package com.master.mosaique_capital.service.banking;

import com.master.mosaique_capital.dto.banking.BankSyncResponse;
import com.master.mosaique_capital.dto.banking.external.ExternalAccountDto;
import com.master.mosaique_capital.dto.banking.external.ExternalTransactionDto;
import com.master.mosaique_capital.entity.BankAccount;
import com.master.mosaique_capital.entity.BankConnection;
import com.master.mosaique_capital.entity.BankTransaction;
import com.master.mosaique_capital.repository.BankAccountRepository;
import com.master.mosaique_capital.repository.BankTransactionRepository;
import com.master.mosaique_capital.service.banking.external.BankAggregationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service de synchronisation des comptes et transactions bancaires
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BankAccountSyncService {

    private final BankAggregationService bankAggregationService;
    private final BankAccountRepository bankAccountRepository;
    private final BankTransactionRepository bankTransactionRepository;
    private final TransactionCategorizationService categorizationService;

    /**
     * Synchronise les comptes et transactions d'une connexion bancaire
     */
    @Transactional
    public BankSyncResponse syncAccountsForConnection(BankConnection connection) {
        log.info("Début de synchronisation pour la connexion ID: {}", connection.getId());

        try {
            // Récupération des comptes depuis le provider externe
            List<ExternalAccountDto> externalAccounts = bankAggregationService.getAccounts(connection.getConnectionId());

            int accountsSynced = 0;
            int transactionsSynced = 0;
            int newAccounts = 0;
            int updatedAccounts = 0;
            int newTransactions = 0;
            int categorizedTransactions = 0;

            for (ExternalAccountDto externalAccount : externalAccounts) {
                // Synchronisation du compte
                BankAccount account = syncAccount(connection, externalAccount);
                accountsSynced++;

                if (account.getCreatedAt().equals(account.getUpdatedAt())) {
                    newAccounts++;
                } else {
                    updatedAccounts++;
                }

                // Synchronisation des transactions (30 derniers jours)
                List<ExternalTransactionDto> externalTransactions =
                        bankAggregationService.getTransactions(connection.getConnectionId(), externalAccount.getExternalId(), 30);

                for (ExternalTransactionDto externalTransaction : externalTransactions) {
                    BankTransaction transaction = syncTransaction(account, externalTransaction);
                    if (transaction != null) {
                        transactionsSynced++;

                        if (transaction.getCreatedAt().equals(LocalDateTime.now().withNano(0))) {
                            newTransactions++;
                        }

                        // Catégorisation automatique des nouvelles transactions
                        if (transaction.getCategory() == null || transaction.getCategory().isEmpty()) {
                            String category = categorizationService.categorizeTransaction(transaction);
                            if (category != null) {
                                transaction.setCategory(category);
                                bankTransactionRepository.save(transaction);
                                categorizedTransactions++;
                            }
                        }
                    }
                }
            }

            BankSyncResponse.SyncStatistics statistics = BankSyncResponse.SyncStatistics.builder()
                    .newAccounts(newAccounts)
                    .updatedAccounts(updatedAccounts)
                    .newTransactions(newTransactions)
                    .categorizedTransactions(categorizedTransactions)
                    .build();

            log.info("Synchronisation terminée pour la connexion ID: {}. Comptes: {}, Transactions: {}",
                    connection.getId(), accountsSynced, transactionsSynced);

            return BankSyncResponse.builder()
                    .connectionId(connection.getId())
                    .success(true)
                    .message("Synchronisation réussie")
                    .accountsSynced(accountsSynced)
                    .transactionsSynced(transactionsSynced)
                    .syncTimestamp(LocalDateTime.now())
                    .statistics(statistics)
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors de la synchronisation de la connexion {}: {}", connection.getId(), e.getMessage(), e);
            return BankSyncResponse.error(connection.getId(), e.getMessage());
        }
    }


    private BankAccount syncAccount(BankConnection connection, ExternalAccountDto externalAccount) {
        Optional<BankAccount> existingAccount = bankAccountRepository
                .findByConnectionAndAccountId(connection, externalAccount.getExternalId());

        BankAccount account;
        if (existingAccount.isPresent()) {
            // Mise à jour du compte existant
            account = existingAccount.get();
            account.setName(externalAccount.getName());
            account.setType(externalAccount.getType());
            account.setBalance(externalAccount.getBalance());
            account.setCurrency(externalAccount.getCurrency());
            // ✅ AJOUT : Mise à jour de l'IBAN
            account.setIban(externalAccount.getIban());
            account.setLastSyncAt(LocalDateTime.now());
        } else {
            // Création d'un nouveau compte
            account = new BankAccount();
            account.setConnection(connection);
            account.setAccountId(externalAccount.getExternalId());
            account.setName(externalAccount.getName());
            account.setType(externalAccount.getType());
            account.setBalance(externalAccount.getBalance());
            account.setCurrency(externalAccount.getCurrency());
            // ✅ AJOUT : Définition de l'IBAN pour les nouveaux comptes
            account.setIban(externalAccount.getIban());
            account.setLastSyncAt(LocalDateTime.now());
        }

        return bankAccountRepository.save(account);
    }
    /**
     * Synchronise une transaction bancaire
     */
    private BankTransaction syncTransaction(BankAccount account, ExternalTransactionDto externalTransaction) {
        // Vérification si la transaction existe déjà
        Optional<BankTransaction> existingTransaction = bankTransactionRepository
                .findByAccountAndTransactionId(account, externalTransaction.getExternalId());

        if (existingTransaction.isPresent()) {
            // Transaction déjà existante, pas de modification
            return null;
        }

        // Création d'une nouvelle transaction
        BankTransaction transaction = new BankTransaction();
        transaction.setAccount(account);
        transaction.setTransactionId(externalTransaction.getExternalId());
        transaction.setAmount(externalTransaction.getAmount());
        transaction.setCurrency(account.getCurrency());
        transaction.setDescription(externalTransaction.getDescription());
        transaction.setTransactionDate(externalTransaction.getTransactionDate());
        transaction.setValueDate(externalTransaction.getValueDate());
        transaction.setCategory(externalTransaction.getCategory());

        return bankTransactionRepository.save(transaction);
    }
}
