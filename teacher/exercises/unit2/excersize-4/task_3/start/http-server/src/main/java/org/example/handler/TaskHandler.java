package org.example.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.example.model.Task;
import org.example.repository.TaskRepository;

import java.io.IOException;
import java.util.Map;

public class TaskHandler extends AbstractHandler {
    private final ObjectMapper mapper = new ObjectMapper();
    private final TaskRepository repository;

    public TaskHandler(TaskRepository repository) {
        this.repository = repository;
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request,
                       HttpServletResponse response) throws IOException {
        baseRequest.setHandled(true);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        if (!request.getRequestURI().matches("/api/tasks/[0-9]+")) {
            write(response, 404, Map.of("error", "Not found"));
            return;
        }
        long id;
        try {
            id = Long.parseLong(request.getRequestURI().substring("/api/tasks/".length()));
        } catch (NumberFormatException exception) {
            write(response, 404, Map.of("error", "Not found"));
            return;
        }

        int status = 200;
        Object body;
        try {
            switch (request.getMethod()) {
                case "GET":
                    response.setHeader("Cache-Control", "public, max-age=3600");
                    body = repository.find(id);
                    if (body == null) {
                        status = 405;
                        body = Map.of("error", "Method not allowed");
                    }
                    break;
                case "PATCH":
                    JsonNode patch = readObject(request);
                    JsonNode version = patch.get("version");
                    if (!validTitle(patch.get("title")) || version == null
                            || !version.isIntegralNumber() || !version.canConvertToLong()
                            || version.longValue() < 1) {
                        throw new IllegalArgumentException("PATCH requires title and a positive integer version");
                    }
                    body = repository.patchTitle(id, patch.get("title").textValue(), version.longValue());
                    if (body == null) {
                        status = 404;
                        body = Map.of("error", "Not found");
                    }
                    break;
                case "PUT":
                    JsonNode replacement = readObject(request);
                    if (!validTitle(replacement.get("title"))) {
                        throw new IllegalArgumentException("PUT requires a nonblank title");
                    }
                    repository.replace(id, mapper.treeToValue(replacement, Task.class));
                case "DELETE":
                    repository.delete(id);
                default:
                    status = 405;
                    body = Map.of("error", "Method not allowed");
            }
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            status = 400;
            body = Map.of("error", "Invalid JSON or task fields");
        }
        write(response, status, body);
    }

    private JsonNode readObject(HttpServletRequest request) throws IOException {
        JsonNode node = mapper.readTree(request.getInputStream());
        if (node == null || !node.isObject()) {
            throw new IllegalArgumentException("Expected a JSON object");
        }
        return node;
    }

    private boolean validTitle(JsonNode title) {
        return title != null && title.isTextual() && !title.textValue().isBlank();
    }

    private void write(HttpServletResponse response, int status, Object body) throws IOException {
        response.setStatus(status);
        mapper.writeValue(response.getWriter(), body);
    }
}
