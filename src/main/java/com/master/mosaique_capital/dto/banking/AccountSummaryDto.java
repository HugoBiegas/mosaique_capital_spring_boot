// ===== AccountSummaryDto.java =====
package com.master.mosaique_capital.dto.banking;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
public class AccountSummaryDto {
    private BigDecimal totalBalance;
    private BigDecimal totalAssets;
    private BigDecimal totalLiabilities;
    private int totalAccounts;
    private Map<String, BigDecimal> balanceByType;
    private Map<String, Integer> accountCountByType;
    private BigDecimal monthlyIncome;
    private BigDecimal monthlyExpenses;
    private BigDecimal netWorth;
}