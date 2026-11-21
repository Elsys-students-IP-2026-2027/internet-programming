package org.example.handler;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.example.model.Task;
import org.example.repository.TaskRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

public final class TaskHandler extends AbstractHandler {
    private static final String ALLOW = "GET, PATCH, DELETE";
    private static final Set<String> PATCH_FIELDS = Set.of("title", "version");
    private final ObjectMapper json = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
    private final TaskRepository repository;

    public TaskHandler(TaskRepository repository) {
        this.repository = repository;
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request,
                       HttpServletResponse response) throws IOException {
        baseRequest.setHandled(true);
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Cache-Control", "no-store");

        Long id = taskId(target);
        if (id == null) {
            error(response, 404, "Непознат път.");
            return;
        }

        switch (request.getMethod()) {
            case "GET" -> {
                Task task = repository.find(id);
                if (task == null) {
                    error(response, 404, "Задачата не съществува.");
                } else {
                    writeJson(response, 200, task);
                }
            }
            case "PATCH" -> patch(id, request, response);
            case "DELETE" -> {
                if (repository.delete(id)) {
                    response.setStatus(204);
                } else {
                    error(response, 404, "Задачата не съществува.");
                }
            }
            default -> {
                // PUT не се поддържа: този API предлага само частична редакция на title.
                response.setHeader("Allow", ALLOW);
                error(response, 405, "Позволени методи: " + ALLOW);
            }
        }
    }

    private void patch(long id, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String contentType = request.getContentType();
        if (contentType == null
                || !contentType.split(";", 2)[0].trim().equalsIgnoreCase("application/json")) {
            error(response, 415, "Изпратете Content-Type: application/json; charset=UTF-8.");
            return;
        }

        JsonNode body;
        try {
            body = json.readTree(request.getReader());
        } catch (JsonProcessingException e) {
            error(response, 400, "Невалиден JSON: нужен е един обект без повторени ключове.");
            return;
        }
        if (body == null || !body.isObject()) {
            error(response, 400, "Тялото трябва да е JSON обект с title и version.");
            return;
        }
        var fields = body.fieldNames();
        while (fields.hasNext()) {
            if (!PATCH_FIELDS.contains(fields.next())) {
                error(response, 400, "Поддържат се само полетата title и version.");
                return;
            }
        }

        JsonNode title = body.get("title");
        if (title == null || !title.isTextual() || title.textValue().isBlank()) {
            error(response, 400, "title трябва да е непразен текст.");
            return;
        }
        JsonNode version = body.get("version");
        if (version == null || !version.isIntegralNumber() || !version.canConvertToLong()
                || version.longValue() < 1 || version.longValue() == Long.MAX_VALUE) {
            error(response, 400, "version трябва да е положително цяло число под 9223372036854775807.");
            return;
        }

        TaskRepository.PatchResult result =
                repository.patchTitle(id, title.textValue(), version.longValue());
        switch (result.status()) {
            case UPDATED -> writeJson(response, 200, result.task());
            case NOT_FOUND -> error(response, 404, "Задачата не съществува.");
            case CONFLICT -> error(response, 409, "Конфликт на версии. Прочетете задачата отново с GET.");
        }
    }

    private Long taskId(String path) {
        if (!path.matches("/api/tasks/[1-9][0-9]*")) {
            return null;
        }
        try {
            return Long.parseLong(path.substring("/api/tasks/".length()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void error(HttpServletResponse response, int status, String message) throws IOException {
        writeJson(response, status, Map.of("error", message));
    }

    private void writeJson(HttpServletResponse response, int status, Object body) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json; charset=UTF-8");
        json.writeValue(response.getWriter(), body);
    }
}
