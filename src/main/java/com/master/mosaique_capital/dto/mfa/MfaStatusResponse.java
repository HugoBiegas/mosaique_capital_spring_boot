// com/master/mosaique_capital/dto/mfa/MfaStatusResponse.java
package com.master.mosaique_capital.dto.mfa;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MfaStatusResponse {
    private boolean enabled;
    private String message;
}