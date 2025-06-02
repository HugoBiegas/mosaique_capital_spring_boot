// com/master/mosaique_capital/security/JwtTokenProvider.java
package com.master.mosaique_capital.security;

import com.master.mosaique_capital.service.auth.TokenBlacklistService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Base64;
import java.util.Date;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenProvider {

    @Value("${app.jwt.secret}")
    private String secretKey;

    @Value("${app.jwt.expiration}")
    private long validityInMilliseconds;

    @Value("${app.jwt.refresh-expiration}")
    private long refreshValidityInMilliseconds;

    private final UserDetailsService userDetailsService;
    private final TokenBlacklistService tokenBlacklistService;

    private Key key;

    @PostConstruct
    protected void init() {
        secretKey = Base64.getEncoder().encodeToString(secretKey.getBytes());
        key = Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    public String createToken(String username) {
        Claims claims = Jwts.claims().setSubject(username);

        Date now = new Date();
        Date validity = new Date(now.getTime() + validityInMilliseconds);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String createRefreshToken(String username) {
        Claims claims = Jwts.claims().setSubject(username);

        Date now = new Date();
        Date validity = new Date(now.getTime() + refreshValidityInMilliseconds);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Authentication getAuthentication(String token) {
        UserDetails userDetails = this.userDetailsService.loadUserByUsername(getUsername(token));
        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }

    public String getUsername(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody().getSubject();
    }

    /**
     * Obtient la date d'expiration d'un token
     */
    public Date getExpirationDate(String token) {
        try {
            return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody().getExpiration();
        } catch (JwtException e) {
            log.warn("Impossible d'extraire la date d'expiration du token: {}", e.getMessage());
            return new Date(); // Retourne la date actuelle si erreur
        }
    }

    /**
     * Obtient l'ID du token (jti) si présent
     */
    public String getTokenId(String token) {
        try {
            return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody().getId();
        } catch (JwtException e) {
            log.debug("Pas d'ID de token trouvé: {}", e.getMessage());
            return null;
        }
    }

    public String resolveToken(HttpServletRequest req) {
        String bearerToken = req.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * Valide un token JWT en vérifiant sa signature, son expiration et la blacklist
     */
    public boolean validateToken(String token) {
        try {
            // Vérification de la blacklist en premier (plus rapide)
            if (tokenBlacklistService.isTokenBlacklisted(token)) {
                log.debug("Token refusé: présent dans la blacklist");
                return false;
            }

            // Vérification de la signature et de l'expiration
            Jws<Claims> claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            Date expiration = claims.getBody().getExpiration();

            if (expiration.before(new Date())) {
                log.debug("Token expiré");
                return false;
            }

            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Token invalide: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Révoque un token en l'ajoutant à la blacklist
     */
    public void revokeToken(String token) {
        try {
            Date expirationDate = getExpirationDate(token);
            tokenBlacklistService.blacklistToken(token, expirationDate);
            log.info("Token révoqué avec succès");
        } catch (Exception e) {
            log.error("Erreur lors de la révocation du token: {}", e.getMessage(), e);
        }
    }

    /**
     * Vérifie si un token est révoqué
     */
    public boolean isTokenRevoked(String token) {
        return tokenBlacklistService.isTokenBlacklisted(token);
    }

    /**
     * Obtient les informations du token sans validation (pour debug)
     */
    public Claims getTokenClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException e) {
            log.warn("Impossible de parser les claims du token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Vérifie si un token expire bientôt (dans les 5 minutes)
     */
    public boolean isTokenExpiringSoon(String token) {
        try {
            Date expiration = getExpirationDate(token);
            long fiveMinutesFromNow = System.currentTimeMillis() + (5 * 60 * 1000);
            return expiration.getTime() < fiveMinutesFromNow;
        } catch (Exception e) {
            return true; // En cas d'erreur, considérer comme expirant
        }
    }

    /**
     * Calcule le temps restant avant expiration en secondes
     */
    public long getTimeToExpiration(String token) {
        try {
            Date expiration = getExpirationDate(token);
            return Math.max(0, (expiration.getTime() - System.currentTimeMillis()) / 1000);
        } catch (Exception e) {
            return 0;
        }
    }
}