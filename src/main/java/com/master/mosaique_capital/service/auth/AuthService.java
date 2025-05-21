// com/master/mosaique_capital/service/auth/AuthService.java
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
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final CodeVerifier codeVerifier = new DefaultCodeVerifier(new DefaultCodeGenerator(), new SystemTimeProvider());

    @Transactional
    public User registerUser(SignupRequest signupRequest) {
        if (userRepository.existsByUsername(signupRequest.getUsername())) {
            throw new DuplicateResourceException("Ce nom d'utilisateur est déjà utilisé");
        }

        if (userRepository.existsByEmail(signupRequest.getEmail())) {
            throw new DuplicateResourceException("Cet email est déjà utilisé");
        }

        User user = userMapper.toEntity(signupRequest);
        user.setPassword(passwordEncoder.encode(signupRequest.getPassword()));
        user.setRoles(Collections.singleton(Role.ROLE_USER));

        return userRepository.save(user);
    }

    @Transactional
    public JwtResponse authenticateUser(LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

            User user = userRepository.findByUsername(loginRequest.getUsername())
                    .orElseThrow(() -> new InvalidCredentialsException("Utilisateur non trouvé"));

            if (user.isMfaEnabled()) {
                if (loginRequest.getMfaCode() == null || !codeVerifier.isValidCode(user.getMfaSecret(), loginRequest.getMfaCode())) {
                    throw new InvalidCredentialsException("Code 2FA invalide");
                }
            }

            // Mise à jour de la dernière connexion
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);

            String accessToken = tokenProvider.createToken(loginRequest.getUsername());
            String refreshToken = tokenProvider.createRefreshToken(loginRequest.getUsername());

            return new JwtResponse(accessToken, refreshToken, user.getId(), user.getUsername(), user.isMfaEnabled());
        } catch (AuthenticationException e) {
            throw new InvalidCredentialsException("Identifiants invalides");
        }
    }

    @Transactional
    public String generateMfaSecret(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new InvalidCredentialsException("Utilisateur non trouvé"));

        SecretGenerator secretGenerator = new DefaultSecretGenerator();
        String secret = secretGenerator.generate();

        user.setMfaSecret(secret);
        userRepository.save(user);

        return secret;
    }

    @Transactional
    public void enableMfa(String username, String code) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new InvalidCredentialsException("Utilisateur non trouvé"));

        if (user.getMfaSecret() == null) {
            throw new InvalidCredentialsException("Aucun secret 2FA généré");
        }

        if (!codeVerifier.isValidCode(user.getMfaSecret(), code)) {
            throw new InvalidCredentialsException("Code 2FA invalide");
        }

        user.setMfaEnabled(true);
        userRepository.save(user);
    }
}