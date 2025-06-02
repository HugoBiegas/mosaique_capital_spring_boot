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
import com.master.mosaique_capital.service.mfa.MfaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final MfaService mfaService;

    /**
     * Enregistre un nouvel utilisateur
     *
     * @param signupRequest Les données d'inscription
     * @throws DuplicateResourceException Si l'utilisateur existe déjà
     */
    @Transactional
    public void registerUser(SignupRequest signupRequest) {
        log.info("Tentative d'enregistrement pour l'utilisateur: {}", signupRequest.getUsername());

        // Vérification de l'unicité du nom d'utilisateur
        if (userRepository.existsByUsername(signupRequest.getUsername())) {
            log.warn("Tentative d'enregistrement avec un nom d'utilisateur existant: {}", signupRequest.getUsername());
            throw new DuplicateResourceException("Ce nom d'utilisateur est déjà utilisé");
        }

        // Vérification de l'unicité de l'email
        if (userRepository.existsByEmail(signupRequest.getEmail())) {
            log.warn("Tentative d'enregistrement avec un email existant: {}", signupRequest.getEmail());
            throw new DuplicateResourceException("Cet email est déjà utilisé");
        }

        // Création de l'utilisateur
        User user = userMapper.toEntity(signupRequest);
        user.setPassword(passwordEncoder.encode(signupRequest.getPassword()));
        user.setRoles(Collections.singleton(Role.ROLE_USER));

        userRepository.save(user);
        log.info("Utilisateur enregistré avec succès: {}", signupRequest.getUsername());
    }

    /**
     * Authentifie un utilisateur et génère les tokens JWT
     *
     * @param loginRequest Les données de connexion
     * @return La réponse JWT avec les tokens
     * @throws InvalidCredentialsException Si les identifiants sont invalides
     */
    @Transactional
    public JwtResponse authenticateUser(LoginRequest loginRequest) {
        log.info("Tentative d'authentification pour l'utilisateur: {}", loginRequest.getUsername());

        try {
            // Authentification avec Spring Security
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

            // Récupération de l'utilisateur
            User user = userRepository.findByUsername(loginRequest.getUsername())
                    .orElseThrow(() -> new InvalidCredentialsException("Utilisateur non trouvé"));

            // Vérification MFA si activée
            if (user.isMfaEnabled()) {
                if (loginRequest.getMfaCode() == null || loginRequest.getMfaCode().trim().isEmpty()) {
                    log.warn("Code MFA manquant pour l'utilisateur: {}", loginRequest.getUsername());
                    throw new InvalidCredentialsException("Code MFA requis");
                }

                if (!mfaService.verifyMfaCode(loginRequest.getUsername(), loginRequest.getMfaCode())) {
                    log.warn("Code MFA invalide pour l'utilisateur: {}", loginRequest.getUsername());
                    throw new InvalidCredentialsException("Code MFA invalide");
                }
            }

            // Mise à jour de la dernière connexion
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);

            // Génération des tokens
            String accessToken = tokenProvider.createToken(loginRequest.getUsername());
            String refreshToken = tokenProvider.createRefreshToken(loginRequest.getUsername());

            log.info("Authentification réussie pour l'utilisateur: {}", loginRequest.getUsername());
            return new JwtResponse(accessToken, refreshToken, user.getId(), user.getUsername(), user.isMfaEnabled());

        } catch (AuthenticationException e) {
            log.warn("Échec d'authentification pour l'utilisateur: {} - {}", loginRequest.getUsername(), e.getMessage());
            throw new InvalidCredentialsException("Identifiants invalides");
        }
    }

    /**
     * Rafraîchit le token d'accès à partir du refresh token
     *
     * @param refreshToken Le refresh token
     * @return Le nouveau token d'accès
     * @throws InvalidCredentialsException Si le refresh token est invalide
     */
    public String refreshAccessToken(String refreshToken) {
        log.info("Tentative de rafraîchissement de token");

        if (!tokenProvider.validateToken(refreshToken)) {
            log.warn("Refresh token invalide");
            throw new InvalidCredentialsException("Refresh token invalide ou expiré");
        }

        String username = tokenProvider.getUsername(refreshToken);

        // Vérification que l'utilisateur existe toujours
        if (!userRepository.existsByUsername(username)) {
            log.warn("Tentative de rafraîchissement pour un utilisateur inexistant: {}", username);
            throw new InvalidCredentialsException("Utilisateur non trouvé");
        }

        String newAccessToken = tokenProvider.createToken(username);
        log.info("Token rafraîchi avec succès pour l'utilisateur: {}", username);

        return newAccessToken;
    }

    /**
     * Déconnecte un utilisateur (invalide le token côté serveur si nécessaire)
     *
     * @param token Le token à invalider
     */
    public void logout(String token) {
        if (token != null && tokenProvider.validateToken(token)) {
            String username = tokenProvider.getUsername(token);
            log.info("Déconnexion de l'utilisateur: {}", username);
        } else {
            log.warn("Tentative de déconnexion avec un token invalide");
        }
    }

    /**
     * Retourne les informations de l'utilisateur connecté
     *
     * @param token Le token JWT
     * @return Les informations utilisateur
     * @throws InvalidCredentialsException Si le token est invalide
     */
    public Map<String, Object> getCurrentUserInfo(String token) {
        if (token == null || !tokenProvider.validateToken(token)) {
            throw new InvalidCredentialsException("Token invalide");
        }

        String username = tokenProvider.getUsername(token);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new InvalidCredentialsException("Utilisateur non trouvé"));

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("email", user.getEmail());
        userInfo.put("mfaEnabled", user.isMfaEnabled());
        userInfo.put("roles", user.getRoles());
        userInfo.put("active", user.isActive());
        userInfo.put("createdAt", user.getCreatedAt());
        userInfo.put("lastLoginAt", user.getLastLoginAt());

        return userInfo;
    }

    /**
     * Vérifie si un utilisateur existe par son nom d'utilisateur
     *
     * @param username Le nom d'utilisateur
     * @return true si l'utilisateur existe, false sinon
     */
    public boolean userExists(String username) {
        return userRepository.existsByUsername(username);
    }

    /**
     * Vérifie si un email est déjà utilisé
     *
     * @param email L'email à vérifier
     * @return true si l'email est utilisé, false sinon
     */
    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * Active ou désactive un compte utilisateur (admin uniquement)
     *
     * @param username Le nom d'utilisateur
     * @param active Le statut à définir
     */
    @Transactional
    public void setUserActiveStatus(String username, boolean active) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new InvalidCredentialsException("Utilisateur non trouvé"));

        user.setActive(active);
        userRepository.save(user);

        log.info("Statut du compte modifié pour {}: {}", username, active ? "activé" : "désactivé");
    }

    /**
     * Modifie les rôles d'un utilisateur (admin uniquement)
     *
     * @param username Le nom d'utilisateur
     * @param roles Les nouveaux rôles
     */
    @Transactional
    public void updateUserRoles(String username, Set<Role> roles) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new InvalidCredentialsException("Utilisateur non trouvé"));

        user.setRoles(roles);
        userRepository.save(user);

        log.info("Rôles modifiés pour l'utilisateur {}: {}", username, roles);
    }
}