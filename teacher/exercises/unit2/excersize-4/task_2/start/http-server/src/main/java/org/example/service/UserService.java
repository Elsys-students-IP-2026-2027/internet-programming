package org.example.service;


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

    // TODO: patchUser
    // TODO: deleteUse

    public User resetUserTemplate() {
        return new User(null, null, 0, null, "ready");
    }
}
