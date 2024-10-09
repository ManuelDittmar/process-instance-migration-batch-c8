package org.camunda.consulting.migration.configuration;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.camunda.consulting.migration.core.exception.RetrieableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class MigrationProcessorsConfig {

    @Value("${process-instance-migration.thread-pool-size:5}")
    private int threadPoolSize;

    @Value("${process-instance-migration.backoff-minimum:1000}")
    private int backoffMinimum;

    @Value("${process-instance-migration.backoff-maximum:4000}")
    private int backoffMaximum;

    @Bean
    public ExecutorService executorService() {
        return Executors.newFixedThreadPool(threadPoolSize);
    }

    @Bean
    public Retry migrationRetry() {
        Random random = new Random();
        RetryConfig retryConfig = RetryConfig.custom()
                .waitDuration(Duration.ofMillis(backoffMinimum + random.nextInt(backoffMaximum - backoffMinimum))) // Random backoff between 1000 and 4000 ms
                .retryExceptions(RetrieableException.class)
                .build();

        return Retry.of("migrationRetry", retryConfig);
    }
}