// ===== BankConnectionRequest.java =====
package com.master.mosaique_capital.dto.banking;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.Map;

@Data
public class BankConnectionRequest {

    @NotBlank(message = "Le provider est obligatoire")
    private String provider;

    @NotNull(message = "Les identifiants bancaires sont obligatoires")
    private BankCredentials bankCredentials;

    @Data
    public static class BankCredentials {
        @NotBlank(message = "L'identifiant utilisateur est obligatoire")
        private String login;

        @NotBlank(message = "Le mot de passe est obligatoire")
        private String password;

        // Champs optionnels selon la banque
        private String additionalField1;
        private String additionalField2;
        private Map<String, String> extraFields;
    }
}
