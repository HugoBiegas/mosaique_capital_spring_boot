// com/master/mosaique_capital/dto/mfa/MfaVerificationRequest.java
package com.master.mosaique_capital.dto.mfa;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class MfaVerificationRequest {
    @NotBlank(message = "Le code MFA est obligatoire")
    @Pattern(regexp = "\\d{6}", message = "Le code MFA doit contenir exactement 6 chiffres")
    private String code;
}
