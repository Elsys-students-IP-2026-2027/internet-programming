package org.example.repository;

import org.example.model.Task;

import java.util.HashMap;
import java.util.Map;

public final class TaskRepository {
    private final Map<Long, Task> tasks = new HashMap<>();

    public enum Status { UPDATED, NOT_FOUND, CONFLICT }

    public record PatchResult(Status status, Task task) {}

    public TaskRepository() {
        Task initial = Task.initial();
        tasks.put(initial.id(), initial);
    }

    public synchronized Task find(long id) {
        return tasks.get(id);
    }

    public synchronized PatchResult patchTitle(long id, String title, long expectedVersion) {
        Task current = tasks.get(id);
        if (current == null) {
            return new PatchResult(Status.NOT_FOUND, null);
        }
        // Сравнението и записът са една атомарна операция, а не две отделни извиквания.
        if (current.version() != expectedVersion) {
            return new PatchResult(Status.CONFLICT, null);
        }
        Task updated = current.withTitle(title);
        tasks.put(id, updated);
        return new PatchResult(Status.UPDATED, updated);
    }

    // DELETE е безусловно изтриване; проверка на версия се изисква само за PATCH.
    public synchronized boolean delete(long id) {
        return tasks.remove(id) != null;
    }
}
