// ===== BankProviderDto.java =====
package com.master.mosaique_capital.dto.banking;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BankProviderDto {
    private String code;
    private String name;
    private String logo;
    private boolean available;
    private List<String> requiredFields;
    private String description;
    private boolean strongAuthentication;
}
