// com/master/mosaique_capital/config/WebConfig.java - NOUVELLE CONFIGURATION
package com.master.mosaique_capital.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Configuration Web supplémentaire pour les APIs bancaires
 */
@Configuration
@Slf4j
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Intercepteur pour logger les requêtes bancaires
        registry.addInterceptor(new BankingRequestInterceptor())
                .addPathPatterns("/api/banking/**")
                .excludePathPatterns("/api/banking/webhooks/**"); // Exclure les webhooks du logging détaillé
    }

    /**
     * Intercepteur pour logger et monitorer les requêtes bancaires
     */
    public static class BankingRequestInterceptor implements HandlerInterceptor {

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            long startTime = System.currentTimeMillis();
            request.setAttribute("startTime", startTime);

            // Log des requêtes sensibles uniquement en debug
            if (log.isDebugEnabled()) {
                log.debug("🔄 Banking Request: {} {}", request.getMethod(), request.getRequestURI());
            }

            return true;
        }

        @Override
        public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                    Object handler, Exception ex) {
            long startTime = (Long) request.getAttribute("startTime");
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            // Log des performances pour les APIs bancaires
            if (duration > 5000) { // Plus de 5 secondes
                log.warn("⚠️ Requête bancaire lente: {} {} - {}ms",
                        request.getMethod(), request.getRequestURI(), duration);
            } else if (log.isDebugEnabled()) {
                log.debug("✅ Banking Request completed: {} {} - {}ms - Status: {}",
                        request.getMethod(), request.getRequestURI(), duration, response.getStatus());
            }

            // Ajouter des headers de monitoring
            response.setHeader("X-Response-Time", String.valueOf(duration));
            response.setHeader("X-API-Version", "1.0.0");
        }
    }
}
