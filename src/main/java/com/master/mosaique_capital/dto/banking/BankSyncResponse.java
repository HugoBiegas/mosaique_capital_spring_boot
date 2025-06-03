// ===== BankSyncResponse.java =====
package com.master.mosaique_capital.dto.banking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankSyncResponse {
    private Long connectionId;
    private boolean success;
    private String message;
    private int accountsSynced;
    private int transactionsSynced;
    private LocalDateTime syncTimestamp;
    private List<String> errors;
    private SyncStatistics statistics;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SyncStatistics {
        private int newAccounts;
        private int updatedAccounts;
        private int newTransactions;
        private int updatedTransactions;
        private int categorizedTransactions;
    }

    public static BankSyncResponse success(Long connectionId, int accounts, int transactions) {
        return BankSyncResponse.builder()
                .connectionId(connectionId)
                .success(true)
                .message("Synchronisation réussie")
                .accountsSynced(accounts)
                .transactionsSynced(transactions)
                .syncTimestamp(LocalDateTime.now())
                .build();
    }

    public static BankSyncResponse error(Long connectionId, String errorMessage) {
        return BankSyncResponse.builder()
                .connectionId(connectionId)
                .success(false)
                .message(errorMessage)
                .accountsSynced(0)
                .transactionsSynced(0)
                .syncTimestamp(LocalDateTime.now())
                .errors(List.of(errorMessage))
                .build();
    }
}