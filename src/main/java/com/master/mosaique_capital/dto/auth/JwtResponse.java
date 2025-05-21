// com/master/mosaique_capital/dto/auth/JwtResponse.java
package com.master.mosaique_capital.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JwtResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private Long id;
    private String username;
    private boolean mfaEnabled;

    public JwtResponse(String accessToken, String refreshToken, Long id, String username, boolean mfaEnabled) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.id = id;
        this.username = username;
        this.mfaEnabled = mfaEnabled;
    }
}