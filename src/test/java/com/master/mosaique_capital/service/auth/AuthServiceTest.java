// src/test/java/com/master/mosaique_capital/service/auth/AuthServiceTest.java
package com.master.mosaique_capital.service.auth;

import com.master.mosaique_capital.dto.auth.JwtResponse;
import com.master.mosaique_capital.dto.auth.LoginRequest;
import com.master.mosaique_capital.dto.auth.SignupRequest;
import com.master.mosaique_capital.entity.User;
import com.master.mosaique_capital.enums.Role;
import com.master.mosaique_capital.exception.DuplicateResourceException;
import com.master.mosaique_capital.exception.InvalidCredentialsException;
import com.master.mosaique_capital.mapper.UserMapper;
import com.master.mosaique_capital.repository.UserRepository;
import com.master.mosaique_capital.security.JwtTokenProvider;
import dev.samstevens.totp.code.CodeVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private CodeVerifier codeVerifier;

    @InjectMocks
    private AuthService authService;

    private SignupRequest signupRequest;
    private LoginRequest loginRequest;
    private User testUser;

    @BeforeEach
    void setUp() {
        // Configurer manuellement l'injection du CodeVerifier
        ReflectionTestUtils.setField(authService, "codeVerifier", codeVerifier);

        // Initialiser la requête d'inscription
        signupRequest = new SignupRequest();
        signupRequest.setUsername("testuser");
        signupRequest.setEmail("test@example.com");
        signupRequest.setPassword("password123");

        // Initialiser la requête de connexion
        loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");

        // Initialiser l'utilisateur de test
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encoded_password");
        testUser.setRoles(Set.of(Role.ROLE_USER));
        testUser.setMfaEnabled(false);
    }

    @Test
    void registerUser_ShouldCreateUser_WhenUserDoesNotExist() {
        // Given
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userMapper.toEntity(any(SignupRequest.class))).thenReturn(testUser);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = authService.registerUser(signupRequest);

        // Then
        assertNotNull(result);
        assertEquals(testUser.getUsername(), result.getUsername());
        assertEquals(testUser.getEmail(), result.getEmail());
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void registerUser_ShouldThrowException_WhenUsernameExists() {
        // Given
        when(userRepository.existsByUsername(anyString())).thenReturn(true);

        // When & Then
        assertThrows(DuplicateResourceException.class, () -> authService.registerUser(signupRequest));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerUser_ShouldThrowException_WhenEmailExists() {
        // Given
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        // When & Then
        assertThrows(DuplicateResourceException.class, () -> authService.registerUser(signupRequest));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void authenticateUser_ShouldReturnJwtResponse_WhenCredentialsAreValid() {
        // Given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(testUser));
        when(tokenProvider.createToken(anyString())).thenReturn("access_token");
        when(tokenProvider.createRefreshToken(anyString())).thenReturn("refresh_token");

        // When
        JwtResponse result = authService.authenticateUser(loginRequest);

        // Then
        assertNotNull(result);
        assertEquals("access_token", result.getAccessToken());
        assertEquals("refresh_token", result.getRefreshToken());
        assertEquals(testUser.getId(), result.getId());
        assertEquals(testUser.getUsername(), result.getUsername());
        assertFalse(result.isMfaEnabled());
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void authenticateUser_ShouldThrowException_WhenCredentialsAreInvalid() {
        // Given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // When & Then
        assertThrows(InvalidCredentialsException.class, () -> authService.authenticateUser(loginRequest));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void authenticateUser_ShouldValidateMfaCode_WhenMfaIsEnabled() {
        // Given
        testUser.setMfaEnabled(true);
        testUser.setMfaSecret("mfa_secret");
        loginRequest.setMfaCode("123456");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(testUser));
        when(codeVerifier.isValidCode("mfa_secret", "123456")).thenReturn(true);
        when(tokenProvider.createToken(anyString())).thenReturn("access_token");
        when(tokenProvider.createRefreshToken(anyString())).thenReturn("refresh_token");

        // When
        JwtResponse result = authService.authenticateUser(loginRequest);

        // Then
        assertNotNull(result);
        assertTrue(result.isMfaEnabled());
        verify(codeVerifier, times(1)).isValidCode("mfa_secret", "123456");
    }

    @Test
    void authenticateUser_ShouldThrowException_WhenMfaCodeIsInvalid() {
        // Given
        testUser.setMfaEnabled(true);
        testUser.setMfaSecret("mfa_secret");
        loginRequest.setMfaCode("123456");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(testUser));
        when(codeVerifier.isValidCode("mfa_secret", "123456")).thenReturn(false);

        // When & Then
        assertThrows(InvalidCredentialsException.class, () -> authService.authenticateUser(loginRequest));
        verify(codeVerifier, times(1)).isValidCode("mfa_secret", "123456");
    }
}