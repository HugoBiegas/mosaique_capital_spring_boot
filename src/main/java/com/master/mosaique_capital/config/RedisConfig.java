// com/master/mosaique_capital/config/RedisConfig.java
package com.master.mosaique_capital.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@Slf4j
public class RedisConfig {

    @Value("${spring.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.redis.port:6379}")
    private int redisPort;

    @Value("${spring.redis.password:}")
    private String redisPassword;

    @Value("${spring.redis.database:0}")
    private int redisDatabase;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        try {
            RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
            config.setHostName(redisHost);
            config.setPort(redisPort);
            config.setDatabase(redisDatabase);

            if (redisPassword != null && !redisPassword.trim().isEmpty()) {
                config.setPassword(redisPassword);
            }

            LettuceConnectionFactory factory = new LettuceConnectionFactory(config);

            // Test de la connexion au démarrage
            factory.afterPropertiesSet();
            log.info("Connexion Redis configurée avec succès sur {}:{}", redisHost, redisPort);

            return factory;
        } catch (Exception e) {
            log.warn("Impossible de configurer Redis: {}. Le système utilisera le cache mémoire.", e.getMessage());
            // Retourne une factory factice pour éviter les erreurs
            return new LettuceConnectionFactory();
        }
    }

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();

        try {
            template.setConnectionFactory(connectionFactory);

            // Configuration des sérialiseurs
            StringRedisSerializer stringSerializer = new StringRedisSerializer();
            template.setKeySerializer(stringSerializer);
            template.setValueSerializer(stringSerializer);
            template.setHashKeySerializer(stringSerializer);
            template.setHashValueSerializer(stringSerializer);

            template.setDefaultSerializer(stringSerializer);
            template.afterPropertiesSet();

            // Test de la connexion
            template.opsForValue().set("mosaique:test", "connection_test");
            template.delete("mosaique:test");

            log.info("RedisTemplate configuré et testé avec succès");

        } catch (Exception e) {
            log.warn("Erreur lors de la configuration du RedisTemplate: {}", e.getMessage());
        }

        return template;
    }
}