package org.example.repository;

import org.example.model.Task;

import java.util.HashMap;
import java.util.Map;

public class TaskRepository {
    private final Map<Long, Task> tasks = new HashMap<>();

    public TaskRepository() {
        tasks.put(5L, new Task(5L, "Изготвяне на доклад", "Доклад за проекта",
                "in_progress", "high", "Ivan", "2026-11-01", 1L));
    }

    public synchronized Task find(long id) {
        return tasks.get(id);
    }

    public synchronized Task replace(long id, Task replacement) {
        if (!tasks.containsKey(id)) {
            return null;
        }
        tasks.put(id, replacement);
        return replacement;
    }

    public synchronized Task patchTitle(long id, String title, long suppliedVersion) {
        Task current = tasks.get(id);
        if (current == null) {
            return null;
        }

        Task updated = new Task(current.id(), title, current.description(), current.status(),
                current.priority(), current.assignedTo(), current.createdAt(),
                current.version() == null ? 1L : current.version() + 1);
        tasks.put(id, updated);
        return updated;
    }

    public synchronized Task delete(long id) {
        return tasks.remove(id);
    }
}
