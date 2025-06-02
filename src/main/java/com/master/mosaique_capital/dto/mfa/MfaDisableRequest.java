// com/master/mosaique_capital/dto/mfa/MfaDisableRequest.java
package com.master.mosaique_capital.dto.mfa;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class MfaDisableRequest {
    @NotBlank(message = "Le code MFA actuel est obligatoire")
    @Pattern(regexp = "\\d{6}", message = "Le code MFA doit contenir exactement 6 chiffres")
    private String currentMfaCode;
}
