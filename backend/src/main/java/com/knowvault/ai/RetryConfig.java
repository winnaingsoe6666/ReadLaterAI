package com.knowvault.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Retry utility with exponential backoff for AI provider calls.
 */
public class RetryConfig {

    private static final Logger log = LoggerFactory.getLogger(RetryConfig.class);

    private final int maxRetries;
    private final Duration baseDelay;
    private final Duration maxDelay;

    public RetryConfig(int maxRetries, Duration baseDelay, Duration maxDelay) {
        this.maxRetries = maxRetries;
        this.baseDelay = baseDelay;
        this.maxDelay = maxDelay;
    }

    /**
     * Execute with retry. Returns result or throws after max retries exhausted.
     */
    public <T> T execute(Supplier<T> operation, String operationName) {
        Exception lastException = null;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                if (attempt > 0) {
                    Duration delay = calculateDelay(attempt);
                    log.info("[{}] Retry attempt {}/{} after {}ms",
                            operationName, attempt, maxRetries, delay.toMillis());
                    try {
                        Thread.sleep(delay.toMillis());
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during retry", ie);
                    }
                }
                return operation.get();
            } catch (Exception e) {
                lastException = e;
                String errorMsg = e.getMessage();

                // Don't retry on client errors (4xx) except 429 (rate limit)
                if (isNonRetryable(errorMsg)) {
                    log.warn("[{}] Non-retryable error: {}", operationName, errorMsg);
                    throw e;
                }

                log.warn("[{}] Attempt {}/{} failed: {}",
                        operationName, attempt + 1, maxRetries + 1, errorMsg);
            }
        }

        throw new RuntimeException(
                String.format("[%s] All %d attempts failed", operationName, maxRetries + 1),
                lastException);
    }

    private Duration calculateDelay(int attempt) {
        // Exponential backoff: base * 2^attempt, capped at maxDelay
        long delayMs = baseDelay.toMillis() * (1L << attempt);
        delayMs = Math.min(delayMs, maxDelay.toMillis());
        // Add jitter: ±25%
        long jitter = (long) (delayMs * 0.25 * (Math.random() * 2 - 1));
        return Duration.ofMillis(delayMs + jitter);
    }

    private boolean isNonRetryable(String errorMsg) {
        if (errorMsg == null) return false;
        // Don't retry on 400, 401, 403 (but DO retry on 429 rate limit)
        return errorMsg.contains("400") || errorMsg.contains("401") || errorMsg.contains("403");
    }

    /**
     * Default retry config for AI providers.
     */
    public static RetryConfig defaults() {
        return new RetryConfig(3, Duration.ofSeconds(1), Duration.ofSeconds(30));
    }
}
