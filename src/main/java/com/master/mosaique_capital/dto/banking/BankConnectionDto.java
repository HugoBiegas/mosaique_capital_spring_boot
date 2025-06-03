// ===== BankConnectionDto.java =====
package com.master.mosaique_capital.dto.banking;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class BankConnectionDto {
    private Long id;
    private String provider;
    private String connectionId;
    private String connectionStatus;
    private LocalDateTime lastSyncAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<BankAccountDto> accounts;
}
