package org.example.model;

public record Task(
        Long id,
        String title,
        String description,
        String status,
        String priority,
        String assignedTo,
        String createdAt,
        Long version
) {
}
