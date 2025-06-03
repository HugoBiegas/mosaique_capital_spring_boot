// ===== ExternalAccountDto.java =====
package com.master.mosaique_capital.dto.banking.external;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class ExternalAccountDto {
    private String externalId;
    private String name;
    private String type;
    private BigDecimal balance;
    private String currency;
    private String iban;
    private String status;
}
