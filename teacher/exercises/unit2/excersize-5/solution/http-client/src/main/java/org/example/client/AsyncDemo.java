package org.example.client;

import org.example.client.exception.ClientErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/// Част 2. Async client
public class AsyncDemo {
    private static final Logger log = LoggerFactory.getLogger(AsyncDemo.class);

    public static void main(String[] args) {
        UserClient client = new UserClient(args.length > 0 ? args[0] : "http://localhost:8000");

        try {
            confirmRegistration(client).join();
            olderOfTwo(client).join();
            friendlyErrors(client).join();
        } catch (CompletionException e) {
            log.error("Неочаквана грешка: {}", Retry.unwrap(e).getMessage());
        }
    }

    /// 1. thenCompose: each subsequent request depends on the result of the previous one.
    static CompletableFuture<Void> confirmRegistration(UserClient client) {
        return client.createAsync(User.of("Мария Петрова", 16, "maria@elsys.bg", "pending"))
                .thenCompose(user -> client.patchAsync(user.id(), Map.of("status", "confirmed")))
                .thenCompose(user -> client.getByIdAsync(user.id()))
                .thenAccept(user -> log.info("1) Потвърдено записване: {}", user.orElseThrow()));
    }

    /// 2. thenCombine: two parallel GET requests, then combine the results.
    static CompletableFuture<Void> olderOfTwo(UserClient client) {
        User georgi = client.create(User.of("Георги Димитров", 17, "georgi@elsys.bg", "pending"));
        User elena = client.create(User.of("Елена Николова", 15, "elena@elsys.bg", "pending"));

        return client.getByIdAsync(georgi.id())
                .thenCombine(client.getByIdAsync(elena.id()), (a, b) -> {
                    User first = a.orElseThrow();
                    User second = b.orElseThrow();
                    return first.age() >= second.age() ? first : second;
                })
                .thenAccept(older -> log.info("2) По-възрастен е {} ({} г.)", older.name(), older.age()));
    }

    /// 3. handle errors
    static CompletableFuture<Void> friendlyErrors(UserClient client) {
        CompletableFuture<String> missing = client.getByIdAsync(UUID.randomUUID()).handle(AsyncDemo::describe);
        CompletableFuture<String> invalid = client.getByIdAsync("abc").handle(AsyncDemo::describe);

        return missing.thenAcceptBoth(invalid, (m, i) -> {
            log.info("3) Несъществуващ UUID: {}", m);
            log.info("3) Id \"abc\": {}", i);
        });
    }

    private static String describe(Optional<User> user, Throwable error) {
        if (error == null) {
            return user.map(u -> "намерен " + u.name()).orElse("няма такъв потребител (HTTP 404)");
        }
        Throwable cause = Retry.unwrap(error);
        if (cause instanceof ClientErrorException e) {
            return "невалидна заявка (HTTP " + e.getStatus() + "): " + e.getBody();
        }
        return "грешка: " + cause.getMessage();
    }
}
