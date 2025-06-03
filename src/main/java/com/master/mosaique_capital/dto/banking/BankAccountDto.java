// ===== BankAccountDto.java =====
package com.master.mosaique_capital.dto.banking;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class BankAccountDto {
    private Long id;
    private String accountId;
    private String name;
    private String type;
    private BigDecimal balance;
    private String currency;
    private String iban;
    private LocalDateTime lastSyncAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<BankTransactionDto> recentTransactions;
}