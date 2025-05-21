// src/test/java/com/master/mosaique_capital/security/JwtTokenProviderTest.java
package com.master.mosaique_capital.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class JwtTokenProviderTest {

    @Autowired
    private JwtTokenProvider tokenProvider;

    private String username;
    private String token;

    @BeforeEach
    void setUp() {
        username = "testuser";
        token = tokenProvider.createToken(username);
    }

    @Test
    void createToken_ShouldGenerateValidToken() {
        // Given
        // Le token est généré dans le setUp

        // When & Then
        assertNotNull(token);
        assertTrue(tokenProvider.validateToken(token));
        assertEquals(username, tokenProvider.getUsername(token));
    }

    @Test
    void createRefreshToken_ShouldGenerateValidRefreshToken() {
        // When
        String refreshToken = tokenProvider.createRefreshToken(username);

        // Then
        assertNotNull(refreshToken);
        assertTrue(tokenProvider.validateToken(refreshToken));
        assertEquals(username, tokenProvider.getUsername(refreshToken));
    }

    @Test
    void getAuthentication_ShouldReturnValidAuthentication() {
        // When
        Authentication authentication = tokenProvider.getAuthentication(token);

        // Then
        assertNotNull(authentication);
        assertTrue(authentication.isAuthenticated());
        assertEquals(username, authentication.getName());
    }

    @Test
    void resolveToken_ShouldExtractTokenFromAuthorizationHeader() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);

        // When
        String resolvedToken = tokenProvider.resolveToken(request);

        // Then
        assertEquals(token, resolvedToken);
    }

    @Test
    void resolveToken_ShouldReturnNull_WhenNoAuthorizationHeader() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();

        // When
        String resolvedToken = tokenProvider.resolveToken(request);

        // Then
        assertNull(resolvedToken);
    }

    @Test
    void resolveToken_ShouldReturnNull_WhenInvalidAuthorizationHeader() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Invalid " + token);

        // When
        String resolvedToken = tokenProvider.resolveToken(request);

        // Then
        assertNull(resolvedToken);
    }

    @Test
    void validateToken_ShouldReturnFalse_WhenTokenIsInvalid() {
        // Given
        String invalidToken = token + "invalid";

        // When & Then
        assertFalse(tokenProvider.validateToken(invalidToken));
    }
}