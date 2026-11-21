package org.example.client;

import org.example.client.exception.HttpCallException;
import org.example.client.exception.RetriesExhaustedException;
import org.example.client.exception.ServerErrorException;
import org.example.client.exception.ServerUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public final class Retry {
    private static final Logger log = LoggerFactory.getLogger(Retry.class);

    private Retry() {
    }

    /// Repeats only on ServerUnavailable (connection refused / timeout) and 5xx, never on 4xx.
    /// Pauses grow 1, 2, 4, 8 s and use delayedExecutor instead of Thread.sleep.
    public static <T> CompletableFuture<T> withRetry(Supplier<CompletableFuture<T>> call, int maxAttempts) {
        return attempt(call, 1, maxAttempts);
    }

    private static <T> CompletableFuture<T> attempt(Supplier<CompletableFuture<T>> call, int attempt, int maxAttempts) {
        CompletableFuture<T> future;
        try {
            future = call.get();
        } catch (RuntimeException e) {
            future = CompletableFuture.failedFuture(e);
        }

        return future.exceptionallyCompose(error -> {
            Throwable cause = unwrap(error);
            if (!isRetryable(cause)) {
                return CompletableFuture.failedFuture(cause);
            }

            HttpCallException failure = (HttpCallException) cause;
            if (attempt >= maxAttempts) {
                log.error("Опит {}/{} неуспешен: {}, X-Request-Id: {}. Спираме.",
                        attempt, maxAttempts, failure.reason(), failure.getRequestId());
                return CompletableFuture.failedFuture(new RetriesExhaustedException(
                        failure.reason() + " след " + maxAttempts + " опита", failure));
            }

            long delaySeconds = 1L << (attempt - 1);
            log.warn("Опит {}/{} неуспешен: {}, X-Request-Id: {}. Нов опит след {} s",
                    attempt, maxAttempts, failure.reason(), failure.getRequestId(), delaySeconds);

            Executor after = CompletableFuture.delayedExecutor(delaySeconds, TimeUnit.SECONDS);
            return CompletableFuture.supplyAsync(() -> null, after)
                    .thenCompose(ignored -> attempt(call, attempt + 1, maxAttempts));
        });
    }

    private static boolean isRetryable(Throwable error) {
        return error instanceof ServerUnavailableException || error instanceof ServerErrorException;
    }

    /// Errors inside a CompletableFuture chain arrive wrapped in CompletionException.
    public static Throwable unwrap(Throwable error) {
        while (error instanceof CompletionException && error.getCause() != null) {
            error = error.getCause();
        }
        return error;
    }
}
