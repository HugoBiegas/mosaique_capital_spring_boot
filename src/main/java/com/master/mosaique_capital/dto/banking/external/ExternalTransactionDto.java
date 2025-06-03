// ===== ExternalTransactionDto.java =====
package com.master.mosaique_capital.dto.banking.external;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class ExternalTransactionDto {
    private String externalId;
    private BigDecimal amount;
    private String description;
    private LocalDate transactionDate;
    private LocalDate valueDate;
    private String category;
    private String type; // DEBIT, CREDIT
}