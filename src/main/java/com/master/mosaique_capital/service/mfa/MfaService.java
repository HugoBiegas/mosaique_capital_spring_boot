// com/master/mosaique_capital/service/mfa/MfaService.java
package com.master.mosaique_capital.service.mfa;

import com.master.mosaique_capital.dto.mfa.MfaSetupResponse;
import com.master.mosaique_capital.entity.User;
import com.master.mosaique_capital.exception.InvalidCredentialsException;
import com.master.mosaique_capital.exception.ResourceNotFoundException;
import com.master.mosaique_capital.repository.UserRepository;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MfaService {

    private final UserRepository userRepository;
    private final QrCodeService qrCodeService;

    private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final CodeVerifier codeVerifier = new DefaultCodeVerifier(
            new DefaultCodeGenerator(),
            new SystemTimeProvider()
    );

    /**
     * Génère un secret MFA et retourne les informations de configuration
     *
     * @param username Le nom d'utilisateur
     * @return Les informations de configuration MFA
     */
    @Transactional
    public MfaSetupResponse generateMfaSetup(String username) {
        log.info("Génération de la configuration MFA pour l'utilisateur: {}", username);

        User user = findUserByUsername(username);

        // Génération d'un nouveau secret
        String secret = secretGenerator.generate();

        // Sauvegarde temporaire du secret (non activé)
        user.setMfaSecret(secret);
        user.setMfaEnabled(false); // Pas encore activé
        userRepository.save(user);

        // Génération de l'URL TOTP
        String totpUrl = qrCodeService.formatTotpUrl(username, secret);

        // Génération du QR code
        byte[] qrCodeImage = qrCodeService.generateQrCodeImage(totpUrl);

        log.info("Configuration MFA générée avec succès pour l'utilisateur: {}", username);

        return MfaSetupResponse.builder()
                .secret(secret)
                .qrCodeUrl(totpUrl)
                .qrCodeImage(qrCodeImage)
                .build();
    }

    /**
     * Vérifie un code MFA et active la fonctionnalité si valide
     *
     * @param username Le nom d'utilisateur
     * @param code Le code à vérifier
     */
    @Transactional
    public void verifyAndEnableMfa(String username, String code) {
        log.info("Vérification et activation MFA pour l'utilisateur: {}", username);

        User user = findUserByUsername(username);

        if (user.getMfaSecret() == null || user.getMfaSecret().trim().isEmpty()) {
            throw new InvalidCredentialsException("Aucun secret MFA configuré. Veuillez d'abord configurer la MFA.");
        }

        if (!isValidMfaCode(user.getMfaSecret(), code)) {
            log.warn("Tentative de vérification MFA échouée pour l'utilisateur: {}", username);
            throw new InvalidCredentialsException("Code MFA invalide");
        }

        // Activation de la MFA
        user.setMfaEnabled(true);
        userRepository.save(user);

        log.info("MFA activée avec succès pour l'utilisateur: {}", username);
    }

    /**
     * Vérifie un code MFA pour un utilisateur ayant déjà la MFA activée
     *
     * @param username Le nom d'utilisateur
     * @param code Le code à vérifier
     * @return true si le code est valide, false sinon
     */
    public boolean verifyMfaCode(String username, String code) {
        User user = findUserByUsername(username);

        if (!user.isMfaEnabled() || user.getMfaSecret() == null) {
            log.warn("Tentative de vérification MFA pour un utilisateur sans MFA activée: {}", username);
            return false;
        }

        boolean isValid = isValidMfaCode(user.getMfaSecret(), code);

        if (!isValid) {
            log.warn("Code MFA invalide pour l'utilisateur: {}", username);
        }

        return isValid;
    }

    /**
     * Désactive la MFA pour un utilisateur
     *
     * @param username Le nom d'utilisateur
     * @param currentMfaCode Le code MFA actuel pour confirmer l'action
     */
    @Transactional
    public void disableMfa(String username, String currentMfaCode) {
        log.info("Désactivation MFA pour l'utilisateur: {}", username);

        User user = findUserByUsername(username);

        if (!user.isMfaEnabled()) {
            throw new InvalidCredentialsException("La MFA n'est pas activée pour cet utilisateur");
        }

        if (!isValidMfaCode(user.getMfaSecret(), currentMfaCode)) {
            log.warn("Tentative de désactivation MFA échouée - code invalide pour: {}", username);
            throw new InvalidCredentialsException("Code MFA invalide");
        }

        // Désactivation et suppression du secret
        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        userRepository.save(user);

        log.info("MFA désactivée avec succès pour l'utilisateur: {}", username);
    }

    /**
     * Régénère un nouveau secret MFA (en cas de compromission)
     *
     * @param username Le nom d'utilisateur
     * @param currentMfaCode Le code MFA actuel pour confirmer l'action
     * @return Les nouvelles informations de configuration MFA
     */
    @Transactional
    public MfaSetupResponse regenerateMfaSecret(String username, String currentMfaCode) {
        log.info("Régénération du secret MFA pour l'utilisateur: {}", username);

        User user = findUserByUsername(username);

        if (user.isMfaEnabled() && !isValidMfaCode(user.getMfaSecret(), currentMfaCode)) {
            log.warn("Tentative de régénération MFA échouée - code invalide pour: {}", username);
            throw new InvalidCredentialsException("Code MFA actuel invalide");
        }

        // Désactivation temporaire pour forcer une nouvelle validation
        user.setMfaEnabled(false);

        return generateMfaSetup(username);
    }

    /**
     * Vérifie le statut MFA d'un utilisateur
     *
     * @param username Le nom d'utilisateur
     * @return true si la MFA est activée, false sinon
     */
    public boolean isMfaEnabled(String username) {
        User user = findUserByUsername(username);
        return user.isMfaEnabled();
    }

    /**
     * Valide un code TOTP avec le secret fourni
     *
     * @param secret Le secret TOTP
     * @param code Le code à vérifier
     * @return true si le code est valide, false sinon
     */
    private boolean isValidMfaCode(String secret, String code) {
        if (code == null || code.trim().isEmpty()) {
            return false;
        }

        try {
            return codeVerifier.isValidCode(secret, code.trim());
        } catch (Exception e) {
            log.error("Erreur lors de la vérification du code MFA: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Recherche un utilisateur par nom d'utilisateur
     *
     * @param username Le nom d'utilisateur
     * @return L'utilisateur trouvé
     * @throws ResourceNotFoundException Si l'utilisateur n'existe pas
     */
    private User findUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé: " + username));
    }
}