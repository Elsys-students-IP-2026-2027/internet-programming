package org.example.repository;

import lombok.extern.slf4j.Slf4j;
import org.example.model.User;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class UserRepository {

    private final Map<UUID, User> users = new ConcurrentHashMap<>();

    public User save(User user) {
        if (user.getId() == null) {
            user.setId(UUID.randomUUID());
        }
        users.put(user.getId(), user);
        log.debug("Saved user: {}", user);
        return user;
    }

    public Collection<User> findAll() {
        return users.values();
    }

    public Optional<User> findById(UUID id) {
        return Optional.ofNullable(users.get(id));
    }

    public boolean existsById(UUID id) {
        return users.containsKey(id);
    }

    public void clear() {
        users.clear();
    }
}
