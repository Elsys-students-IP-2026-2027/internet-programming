package org.example.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Част 3. Retry.
/// A) Спрете сървъра, пуснете RetryDemo и пуснете сървъра до ~15 s -> успех.
/// B) Не пускайте сървъра -> грешка след 5 опита.
public class RetryDemo {
    private static final Logger log = LoggerFactory.getLogger(RetryDemo.class);
    private static final int MAX_ATTEMPTS = 5;

    public static void main(String[] args) {
        UserClient client = new UserClient(args.length > 0 ? args[0] : "http://localhost:8000");
        User student = User.of("Иван Иванов", 17, "ivan@elsys.bg", "pending");

        // Same X-Request-Id for every attempt: it is one logical request.
        String requestId = UserClient.newRequestId();
        log.info("Записване на {} с X-Request-Id: {} (до {} опита)", student.name(), requestId, MAX_ATTEMPTS);

        Retry.withRetry(() -> client.createAsync(student, requestId), MAX_ATTEMPTS)
                .handle((user, error) -> {
                    if (error == null) {
                        log.info("Успех: {}", user);
                    } else {
                        log.error("Неуспех: {}", Retry.unwrap(error).getMessage());
                    }
                    return null;
                })
                .join();
    }
}
