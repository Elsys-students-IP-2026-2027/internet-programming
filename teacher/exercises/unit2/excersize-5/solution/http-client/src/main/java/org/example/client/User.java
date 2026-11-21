package org.example.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record User(UUID id, String name, int age, String email, String status) {

    public static User of(String name, int age, String email, String status) {
        return new User(null, name, age, email, status);
    }
}
