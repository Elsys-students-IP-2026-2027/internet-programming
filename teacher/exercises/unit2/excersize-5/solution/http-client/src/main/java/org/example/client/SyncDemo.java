package org.example.client;

import org.example.client.exception.ClientErrorException;
import org.example.client.exception.ServerUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/// Part one - sync client
public class SyncDemo {
    private static final Logger log = LoggerFactory.getLogger(SyncDemo.class);

    public static void main(String[] args) {
        UserClient client = new UserClient(args.length > 0 ? args[0] : "http://localhost:8000");

        try {
            User me = client.create(User.of("Иван Иванов", 17, "ivan@elsys.bg", "pending"));
            log.info("Създаден: {}", me);

            User active = client.patch(me.id(), Map.of("status", "active"));
            log.info("След PATCH: {}", active);

            log.info("Изтрит: {}", client.delete(me.id()));
            log.info("getById след изтриване: {}", client.getById(me.id()));
            log.info("Повторно изтриване: {}", client.delete(me.id()));

            // Въпрос 1.2: невалидно поле -> 400 -> ClientErrorException
            User other = client.create(User.of("Петър Петров", 16, "petar@elsys.bg", "pending"));
            try {
                client.patch(other.id(), Map.of("age", "abc"));
            } catch (ClientErrorException e) {
                log.warn("PATCH age=\"abc\" -> ClientErrorException, status {}, body {}", e.getStatus(), e.getBody());
            } finally {
                client.delete(other.id());
            }

            log.info("Всички потребители: {}", client.getAll());
        } catch (ServerUnavailableException e) {
            log.error("Сървърът не е достъпен: {}", e.getMessage());
        }
    }
}
