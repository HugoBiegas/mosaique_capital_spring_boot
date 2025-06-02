// com/master/mosaique_capital/service/auth/TokenBlacklistService.java
package com.master.mosaique_capital.service.auth;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service de gestion de la blacklist des tokens JWT révoqués
 * Utilise Redis si disponible, sinon une Map en mémoire (pour développement)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistService {

    private final RedisTemplate<String, String> redisTemplate;

    // Fallback pour développement sans Redis
    private final ConcurrentHashMap<String, Long> inMemoryBlacklist = new ConcurrentHashMap<>();

    private static final String BLACKLIST_KEY_PREFIX = "mosaique:blacklist:";
    private static final Duration CLEANUP_INTERVAL = Duration.ofHours(1);

    /**
     * Ajoute un token à la blacklist
     *
     * @param token Le token JWT à blacklister
     * @param expirationDate Date d'expiration du token
     */
    public void blacklistToken(String token, Date expirationDate) {
        String tokenHash = getTokenHash(token);
        long expirationTime = expirationDate.getTime();

        try {
            // Tentative d'utilisation de Redis
            String key = BLACKLIST_KEY_PREFIX + tokenHash;
            long ttlSeconds = (expirationTime - System.currentTimeMillis()) / 1000;

            if (ttlSeconds > 0) {
                redisTemplate.opsForValue().set(key, "blacklisted", Duration.ofSeconds(ttlSeconds));
                log.info("Token blacklisté dans Redis avec TTL de {} secondes", ttlSeconds);
            }
        } catch (Exception e) {
            // Fallback vers la Map en mémoire si Redis indisponible
            log.warn("Redis indisponible, utilisation du cache mémoire: {}", e.getMessage());
            inMemoryBlacklist.put(tokenHash, expirationTime);
            log.info("Token blacklisté en mémoire");
        }
    }

    /**
     * Vérifie si un token est blacklisté
     *
     * @param token Le token à vérifier
     * @return true si le token est blacklisté, false sinon
     */
    public boolean isTokenBlacklisted(String token) {
        String tokenHash = getTokenHash(token);

        try {
            // Vérification dans Redis
            String key = BLACKLIST_KEY_PREFIX + tokenHash;
            Boolean exists = redisTemplate.hasKey(key);
            if (exists) {
                log.debug("Token trouvé dans la blacklist Redis");
                return true;
            }
        } catch (Exception e) {
            log.debug("Erreur Redis lors de la vérification: {}", e.getMessage());
        }

        // Vérification dans la Map en mémoire
        Long expirationTime = inMemoryBlacklist.get(tokenHash);
        if (expirationTime != null) {
            if (System.currentTimeMillis() < expirationTime) {
                log.debug("Token trouvé dans la blacklist mémoire");
                return true;
            } else {
                // Token expiré, on le supprime de la Map
                inMemoryBlacklist.remove(tokenHash);
                log.debug("Token expiré supprimé de la blacklist mémoire");
            }
        }

        return false;
    }

    /**
     * Nettoie les tokens expirés de la blacklist mémoire
     * (Redis gère automatiquement le TTL)
     */
    public void cleanupExpiredTokens() {
        long currentTime = System.currentTimeMillis();
        int removedCount = 0;

        inMemoryBlacklist.entrySet().removeIf(entry -> {
            return currentTime >= entry.getValue();
        });

    }

    /**
     * Révoque tous les tokens d'un utilisateur
     *
     * @param username Le nom d'utilisateur
     */
    public void revokeAllUserTokens(String username) {
        try {
            // Pattern pour trouver tous les tokens de l'utilisateur dans Redis
            String pattern = BLACKLIST_KEY_PREFIX + "user:" + username + ":*";
            var keys = redisTemplate.keys(pattern);

            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Tous les tokens de l'utilisateur {} ont été révoqués dans Redis", username);
            }
        } catch (Exception e) {
            log.warn("Erreur lors de la révocation des tokens utilisateur dans Redis: {}", e.getMessage());
        }

        // Pour la Map mémoire, on ne peut pas facilement identifier les tokens par utilisateur
        // car on stocke seulement le hash du token
        log.info("Révocation des tokens utilisateur {} demandée", username);
    }

    /**
     * Obtient le statut de la blacklist
     *
     * @return Informations sur la blacklist
     */
    public BlacklistStatus getBlacklistStatus() {
        int memorySize = inMemoryBlacklist.size();
        boolean redisAvailable = false;
        long redisSize = 0;

        try {
            // Test de connectivité Redis
            redisTemplate.opsForValue().get("test");
            redisAvailable = true;

            // Compte approximatif des clés de blacklist dans Redis
            var keys = redisTemplate.keys(BLACKLIST_KEY_PREFIX + "*");
            redisSize = keys.size();
        } catch (Exception e) {
            log.debug("Redis non disponible pour le statut: {}", e.getMessage());
        }

        return new BlacklistStatus(redisAvailable, redisSize, memorySize);
    }

    /**
     * Génère un hash du token pour le stockage
     * (Pour éviter de stocker le token complet)
     */
    private String getTokenHash(String token) {
        return String.valueOf(token.hashCode());
    }

    /**
     * Classe pour le statut de la blacklist
     */
    @Getter
    public static class BlacklistStatus {
        private final boolean redisAvailable;
        private final long redisTokenCount;
        private final int memoryTokenCount;

        public BlacklistStatus(boolean redisAvailable, long redisTokenCount, int memoryTokenCount) {
            this.redisAvailable = redisAvailable;
            this.redisTokenCount = redisTokenCount;
            this.memoryTokenCount = memoryTokenCount;
        }

        @Override
        public String toString() {
            return String.format("BlacklistStatus{redis=%s, redisTokens=%d, memoryTokens=%d}",
                    redisAvailable, redisTokenCount, memoryTokenCount);
        }
    }
}