package org.example.service;


import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.model.User;
import org.example.repository.UserRepository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;


@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserRepository repository;

    public User createUser(User user) {
        log.debug("Creating user: {}", user);
        return repository.save(user);
    }

    public Collection<User> getAllUsers() {
        return repository.findAll();
    }

    public Optional<User> getUserById(UUID id) {
        return repository.findById(id);
    }

    public Optional<User> replaceUser(UUID id, User newUser) {
        if (!repository.existsById(id)) {
            log.warn("User not found for replace: {}", id);
            return Optional.empty();
        }
        newUser.setId(id);
        repository.save(newUser);
        log.debug("Replaced user: {}", newUser);
        return Optional.of(newUser);
    }

  ///Partial update: only the fields present in the JSON body are changed.
    public synchronized Optional<User> patchUser(UUID id, JsonNode patch) {
        User existing = repository.findById(id).orElse(null);
        if (existing == null) {
            log.warn("User not found for patch: {}", id);
            return Optional.empty();
        }

        User updated = new User(existing.getId(), existing.getName(), existing.getAge(),
                existing.getEmail(), existing.getStatus());
        patch.fields().forEachRemaining(field -> {
            JsonNode value = field.getValue();
            switch (field.getKey()) {
                case "name" -> updated.setName(requireText(field.getKey(), value));
                case "email" -> updated.setEmail(requireText(field.getKey(), value));
                case "status" -> updated.setStatus(requireText(field.getKey(), value));
                case "age" -> {
                    if (!value.isInt() || value.intValue() < 0) {
                        throw new IllegalArgumentException("age must be a non-negative integer");
                    }
                    updated.setAge(value.intValue());
                }
                default -> throw new IllegalArgumentException("Field cannot be patched: " + field.getKey());
            }
        });

        repository.save(updated);
        log.debug("Patched user {} -> {}", id, updated);
        return Optional.of(updated);
    }

    private static String requireText(String name, JsonNode value) {
        if (!value.isTextual()) {
            throw new IllegalArgumentException(name + " must be a string");
        }
        return value.textValue();
    }

    public boolean deleteUser(UUID id) {
        return repository.deleteById(id);
    }

    public User resetUserTemplate() {
        return new User(null, null, 0, null, "ready");
    }
}
