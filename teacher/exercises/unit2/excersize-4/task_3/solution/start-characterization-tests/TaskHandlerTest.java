package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.example.handler.TaskHandler;
import org.example.model.Task;
import org.example.repository.TaskRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

class TaskHandlerTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newHttpClient();
    private TaskRepository repository;
    private Server server;
    private String baseUrl;

    @BeforeEach
    void start() throws Exception {
        repository = new TaskRepository();
        server = new Server(0);
        server.setHandler(new TaskHandler(repository));
        server.start();
        baseUrl = "http://localhost:" + ((ServerConnector) server.getConnectors()[0]).getLocalPort();
    }

    @AfterEach
    void stop() throws Exception {
        server.stop();
    }

    @Test
    void initialGetHasPublicCacheAndCompleteSnapshot() throws Exception {
        HttpResponse<String> response = send("GET", "/api/tasks/5", null);
        assertEquals(200, response.statusCode());
        assertEquals("public, max-age=3600", response.headers().firstValue("Cache-Control").orElseThrow());
        assertTrue(response.headers().firstValue("Content-Type").orElseThrow().startsWith("application/json"));
        assertEquals(repository.find(5), mapper.readValue(response.body(), Task.class));
        assertEquals(new Task(5L, "Изготвяне на доклад", "Доклад за проекта",
                "in_progress", "high", "Ivan", "2026-11-01", 1L), repository.find(5));
    }

    @Test
    void replacementClearsEveryOmittedFieldBeforeDeletion() throws Exception {
        Task replacement = mapper.readValue("{\"title\":\"Нов доклад\"}", Task.class);
        assertEquals(replacement, repository.replace(5, replacement));
        assertEquals(new Task(null, "Нов доклад", null, null, null, null, null, null), repository.find(5));
        assertNotNull(repository.delete(5));
        assertNull(repository.find(5));
    }

    @Test
    void putDeletesTaskAndUltimatelyReturns405() throws Exception {
        assertEquals(405, send("PUT", "/api/tasks/5", "{\"title\":\"Нов доклад\"}").statusCode());
        assertNull(repository.find(5));
        assertEquals(405, send("GET", "/api/tasks/5", null).statusCode());
    }

    @Test
    void deleteDeletesTaskAndUltimatelyReturns405() throws Exception {
        assertEquals(405, send("DELETE", "/api/tasks/5", null).statusCode());
        assertNull(repository.find(5));
        assertEquals(405, send("GET", "/api/tasks/5", null).statusCode());
    }

    @Test
    void stalePatchSucceedsTwiceAndOverwritesFirstUpdate() throws Exception {
        Task initial = repository.find(5);
        HttpResponse<String> first = send("PATCH", "/api/tasks/5", "{\"title\":\"Първи\",\"version\":1}");
        HttpResponse<String> second = send("PATCH", "/api/tasks/5", "{\"title\":\"Втори\",\"version\":1}");
        assertEquals(200, first.statusCode());
        assertEquals(200, second.statusCode());
        assertEquals(2, mapper.readTree(first.body()).get("version").longValue());
        JsonNode updated = mapper.readTree(second.body());
        assertEquals(3, updated.get("version").longValue());
        assertEquals("Втори", updated.get("title").textValue());
        assertEquals(initial.description(), repository.find(5).description());
        assertEquals(1L, initial.version());
        assertEquals(new Task(5L, "Втори", initial.description(), initial.status(),
                initial.priority(), initial.assignedTo(), initial.createdAt(), 3L), repository.find(5));
    }

    @Test
    void invalidBodiesReturn400WithoutChangingTask() throws Exception {
        Task initial = repository.find(5);
        for (String body : new String[]{"{", "", "null", "[]", "{}", "{\"title\":12}",
                "{\"title\":\" \"}", "{\"title\":\"x\",\"version\":\"invalid\"}"}) {
            assertEquals(400, send("PUT", "/api/tasks/5", body).statusCode(), body);
            assertEquals(initial, repository.find(5));
        }
        for (String body : new String[]{"{", "{}", "{\"title\":\"x\"}",
                "{\"title\":\"x\",\"version\":0}", "{\"title\":\"x\",\"version\":1.5}",
                "{\"title\":\"x\",\"version\":9223372036854775808}"}) {
            assertEquals(400, send("PATCH", "/api/tasks/5", body).statusCode(), body);
            assertEquals(initial, repository.find(5));
        }
    }

    @Test
    void routingIsExactAndNumericAndMissingGetIntentionallyReturns405() throws Exception {
        for (String path : new String[]{"/", "/api/tasks", "/api/tasks/5/", "/api/tasks/5/extra",
                "/api/tasks/abc", "/api/tasks/-1", "/api/tasks/9223372036854775808"}) {
            assertEquals(404, send("GET", path, null).statusCode(), path);
        }
        assertEquals(405, send("GET", "/api/tasks/42", null).statusCode());
        assertEquals(404, send("PATCH", "/api/tasks/42", "{\"title\":\"x\",\"version\":1}").statusCode());
        assertEquals(405, send("POST", "/api/tasks/5", "{}").statusCode());
        assertNotNull(repository.find(5));
    }

    @Test
    void newRepositoryResetsData() {
        repository.delete(5);
        assertEquals(1L, new TaskRepository().find(5).version());
    }

    private HttpResponse<String> send(String method, String path, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
