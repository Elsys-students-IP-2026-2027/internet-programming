package org.example.model;

// Неизменим обект: сериализацията не може да види наполовина извършена промяна.
public record Task(long id, String title, String description, String status,
                   String priority, String assignedTo, String createdAt, long version) {
    public static Task initial() {
        return new Task(5, "Изготвяне на доклад", "Доклад за проекта",
                "in_progress", "high", "Ivan", "2026-11-01", 1);
    }

    public Task withTitle(String newTitle) {
        return new Task(id, newTitle, description, status, priority, assignedTo, createdAt, version + 1);
    }
}
