// ===== TransactionSearchCriteria.java =====
package com.master.mosaique_capital.dto.banking;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TransactionSearchCriteria {
    private Long accountId;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private String category;
    private String type; // DEBIT, CREDIT
    private String description;
    private int page = 0;
    private int size = 50;
    private String sortBy = "transactionDate";
    private String sortDirection = "DESC";
}
