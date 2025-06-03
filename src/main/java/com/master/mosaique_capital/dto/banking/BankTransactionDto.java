// ===== BankTransactionDto.java =====
package com.master.mosaique_capital.dto.banking;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class BankTransactionDto {
    private Long id;
    private String transactionId;
    private BigDecimal amount;
    private String currency;
    private String description;
    private LocalDate transactionDate;
    private LocalDate valueDate;
    private String category;
    private String type; // DEBIT, CREDIT
    private LocalDateTime createdAt;
}
