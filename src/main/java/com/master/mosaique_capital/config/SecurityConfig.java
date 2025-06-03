// com/master/mosaique_capital/config/SecurityConfig.java
package com.master.mosaique_capital.config;

import com.master.mosaique_capital.security.JwtAuthenticationFilter;
import com.master.mosaique_capital.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider tokenProvider;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        // ===== ENDPOINTS PUBLICS =====
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()

                        // ===== WEBHOOKS BANCAIRES (publics mais sécurisés par signature) =====
                        .requestMatchers("/api/banking/webhooks/**").permitAll()

                        // ===== ENDPOINTS BANKING (authentifiés) =====
                        .requestMatchers("/api/banking/providers").hasRole("USER")
                        .requestMatchers("/api/banking/connections/**").hasRole("USER")
                        .requestMatchers("/api/banking/accounts/**").hasRole("USER")
                        .requestMatchers("/api/banking/transactions/**").hasRole("USER")
                        .requestMatchers("/api/banking/summary").hasRole("USER")
                        .requestMatchers("/api/banking/status").hasRole("USER")

                        // ===== ENDPOINTS ASSETS (authentifiés) =====
                        .requestMatchers("/api/assets/**").hasRole("USER")
                        .requestMatchers("/api/valuations/**").hasRole("USER")
                        .requestMatchers("/api/portfolio/**").hasRole("USER")

                        // ===== ENDPOINTS MFA (authentifiés) =====
                        .requestMatchers("/api/mfa/**").hasRole("USER")

                        // ===== ADMIN ENDPOINTS (réservés aux admins) =====
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // ===== TOUS LES AUTRES ENDPOINTS =====
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(new JwtAuthenticationFilter(tokenProvider), UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // ✅ Origins autorisés - UNIQUEMENT localhost avec différents ports
        configuration.setAllowedOriginPatterns(Arrays.asList(
                "http://localhost:*",           // Tous les ports localhost HTTP
                "https://localhost:*",          // Tous les ports localhost HTTPS
                "http://127.0.0.1:*",          // IPv4 localhost
                "https://127.0.0.1:*"          // IPv4 localhost HTTPS
        ));

        // ✅ Méthodes HTTP autorisées
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD"
        ));

        // ✅ Headers autorisés (ajout des headers bancaires)
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers",
                "X-Requested-With",
                "Cache-Control",
                "X-Webhook-Signature",         // Pour Budget Insight
                "X-Linxo-Signature",          // Pour Linxo
                "X-Banking-Provider"          // Header custom pour identifier le provider
        ));

        // ✅ Headers exposés au client
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Disposition",
                "Content-Length",
                "X-Total-Count",
                "X-Sync-Status",              // Pour le statut de synchronisation
                "X-Last-Sync"                 // Pour la date de dernière sync
        ));

        // ✅ Autoriser les credentials (cookies, tokens)
        configuration.setAllowCredentials(true);

        // ✅ Cache preflight requests pour 1 heure
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}