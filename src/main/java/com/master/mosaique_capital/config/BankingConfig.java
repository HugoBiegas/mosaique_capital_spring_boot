// com/master/mosaique_capital/config/BankingConfig.java
package com.master.mosaique_capital.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.Executor;

/**
 * Configuration pour les services bancaires et d'agrégation
 */
@Configuration
@EnableScheduling
@EnableAsync
@Slf4j
public class BankingConfig {

    @Value("${app.banking.request-timeout:30000}")
    private int requestTimeout;

    @Value("${app.banking.connection-timeout:10000}")
    private int connectionTimeout;

    @Value("${app.banking.async.core-pool-size:5}")
    private int corePoolSize;

    @Value("${app.banking.async.max-pool-size:10}")
    private int maxPoolSize;

    @Value("${app.banking.async.queue-capacity:25}")
    private int queueCapacity;

    /**
     * RestTemplate configuré pour les appels aux APIs bancaires externes
     */
    @Bean(name = "bankingRestTemplate")
    public RestTemplate bankingRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(clientHttpRequestFactory());

        log.info("RestTemplate bancaire configuré avec timeout: {}ms", requestTimeout);
        return restTemplate;
    }

    /**
     * Configuration des timeouts pour les requêtes HTTP
     */
    @Bean
    public ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectionTimeout);
        factory.setReadTimeout(requestTimeout);
        return factory;
    }

    /**
     * Executor pour les tâches asynchrones bancaires
     */
    @Bean(name = "bankingTaskExecutor")
    public Executor bankingTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("Banking-Async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();

        log.info("Thread pool bancaire configuré: core={}, max={}, queue={}",
                corePoolSize, maxPoolSize, queueCapacity);

        return executor;
    }
}
