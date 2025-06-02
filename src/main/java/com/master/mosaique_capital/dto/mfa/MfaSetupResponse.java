// com/master/mosaique_capital/dto/mfa/MfaSetupResponse.java
package com.master.mosaique_capital.dto.mfa;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MfaSetupResponse {
    private String secret;
    private String qrCodeUrl;
    private byte[] qrCodeImage;
}