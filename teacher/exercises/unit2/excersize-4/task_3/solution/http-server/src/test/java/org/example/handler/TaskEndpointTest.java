package org.example.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.example.model.Task;
import org.example.repository.TaskRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class TaskEndpointTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String PATCH = "{\"title\":\"Готов доклад\",\"version\":1}";
    private HttpClient client;
    private Server server;
    private URI base;
    private TaskRepository repository;

    @BeforeEach
    void start() throws Exception {
        repository = new TaskRepository();
        server = new Server(0);
        server.setHandler(new TaskHandler(repository));
        server.start();
        int port = ((ServerConnector) server.getConnectors()[0]).getLocalPort();
        base = URI.create("http://localhost:" + port);
        client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    @AfterEach
    void stop() throws Exception {
        if (client != null) {
            client.close();
        }
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void initialGetHasExactFieldsAndDisallowsCaching() throws Exception {
        HttpResponse<String> response = send("GET", "/api/tasks/5", null, null);
        assertEquals(200, response.statusCode());
        assertJsonUtf8(response);
        assertEquals("no-store", response.headers().firstValue("Cache-Control").orElseThrow());
        assertEquals(JSON.readTree(JSON.writeValueAsString(Task.initial())), JSON.readTree(response.body()));
    }

    @Test
    void patchPreservesAllOtherFieldsAndReturnsAnImmutableSnapshot() throws Exception {
        Task snapshot = repository.find(5);
        HttpResponse<String> patched = send("PATCH", "/api/tasks/5", PATCH,
                "Application/JSON; charset=UTF-8");
        assertEquals(200, patched.statusCode());
        assertJsonUtf8(patched);
        assertEquals(JSON.readTree(JSON.writeValueAsString(Task.initial().withTitle("Готов доклад"))),
                JSON.readTree(patched.body()));
        assertEquals(Task.initial(), snapshot);
        assertEquals(patched.body(), send("GET", "/api/tasks/5", null, null).body());
    }

    @Test
    void staleVersionReturnsConflictWithoutOverwriting() throws Exception {
        HttpResponse<String> first = send("PATCH", "/api/tasks/5", PATCH, "application/json");
        assertEquals(200, first.statusCode());
        assertError(send("PATCH", "/api/tasks/5",
                "{\"title\":\"Стара редакция\",\"version\":1}", "application/json"), 409);
        assertEquals(first.body(), send("GET", "/api/tasks/5", null, null).body());
        HttpResponse<String> retry = send("PATCH", "/api/tasks/5",
                "{\"title\":\"Слята редакция\",\"version\":2}", "application/json");
        assertEquals(200, retry.statusCode());
        assertEquals(3, JSON.readTree(retry.body()).path("version").asLong());
    }

    @Test
    void twoCompetingRequestsHaveExactlyOneWinner() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var first = pool.submit(() -> {
                ready.countDown();
                assertTrue(go.await(5, TimeUnit.SECONDS));
                return send("PATCH", "/api/tasks/5",
                        "{\"title\":\"Първи редактор\",\"version\":1}", "application/json");
            });
            var second = pool.submit(() -> {
                ready.countDown();
                assertTrue(go.await(5, TimeUnit.SECONDS));
                return send("PATCH", "/api/tasks/5",
                        "{\"title\":\"Втори редактор\",\"version\":1}", "application/json");
            });
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            go.countDown();
            var a = first.get(10, TimeUnit.SECONDS);
            var b = second.get(10, TimeUnit.SECONDS);
            assertEquals(List.of(200, 409),
                    List.of(a.statusCode(), b.statusCode()).stream().sorted().toList());
            HttpResponse<String> winner = a.statusCode() == 200 ? a : b;
            assertError(a.statusCode() == 409 ? a : b, 409);
            assertEquals(winner.body(), send("GET", "/api/tasks/5", null, null).body());
            assertEquals(2, repository.find(5).version());
        }
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {
            "", " ", "{", "{\"title\":", "null", "[]", "true", "\"текст\"", "1",
            "{}", "{\"version\":1}", "{\"title\":\"\",\"version\":1}",
            "{\"title\":\"  \\t\\n\",\"version\":1}", "{\"title\":null,\"version\":1}",
            "{\"title\":23,\"version\":1}", "{\"title\":{},\"version\":1}",
            "{\"title\":\"Доклад\"}", "{\"title\":\"Доклад\",\"version\":null}",
            "{\"title\":\"Доклад\",\"version\":0}", "{\"title\":\"Доклад\",\"version\":-1}",
            "{\"title\":\"Доклад\",\"version\":1.0}", "{\"title\":\"Доклад\",\"version\":\"1\"}",
            "{\"title\":\"Доклад\",\"version\":true}", "{\"title\":\"Доклад\",\"version\":[]}",
            "{\"title\":\"Доклад\",\"version\":9223372036854775807}",
            "{\"title\":\"Доклад\",\"version\":9223372036854775808}",
            "{\"title\":\"Доклад\",\"version\":1,\"id\":5}",
            "{\"title\":\"Доклад\",\"version\":1,\"priority\":\"low\"}",
            "{\"title\":\"Доклад\",\"version\":1,\"unknown\":null}",
            "{\"title\":\"Доклад\",\"version\":1} {}",
            "{\"title\":\"Доклад\",\"version\":1} null",
            "{\"title\":\"Доклад\",\"title\":\"Друго\",\"version\":1}",
            "{\"title\":\"Доклад\",\"version\":1,\"version\":2}"
    })
    void invalidJsonOrFieldsAreRejectedWithoutMutation(String body) throws Exception {
        assertError(send("PATCH", "/api/tasks/5", body, "application/json"), 400);
        assertEquals(Task.initial(), repository.find(5));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"text/plain", "application/json-extra", "application/octet-stream"})
    void patchRequiresJsonContentTypeBeforeParsing(String contentType) throws Exception {
        assertError(send("PATCH", "/api/tasks/5", PATCH, contentType), 415);
        assertError(send("PATCH", "/api/tasks/5", "{", contentType), 415);
        assertEquals(Task.initial(), repository.find(5));
    }

    @ParameterizedTest
    @ValueSource(strings = {"PUT", "POST", "OPTIONS", "HEAD"})
    void unsupportedMethodsReturnAllowWithoutSideEffects(String method) throws Exception {
        HttpResponse<String> response = send(method, "/api/tasks/5",
                method.equals("HEAD") ? null : "{\"title\":\"Ново заглавие\"}", "application/json");
        assertEquals(405, response.statusCode());
        assertEquals("GET, PATCH, DELETE", response.headers().firstValue("Allow").orElseThrow());
        if (method.equals("HEAD")) {
            assertEquals("", response.body());
        } else {
            assertError(response, 405);
        }
        assertEquals(Task.initial(), repository.find(5));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/tasks", "/api/tasks/", "/api/tasks/5/",
            "/api/tasks/5/extra", "/api/tasks/05", "/api/tasks/0", "/api/tasks/-1",
            "/api/tasks/five", "/API/tasks/5", "/api/tasks/9223372036854775808"})
    void invalidPathsReturn404RegardlessOfMethod(String path) throws Exception {
        assertError(send("GET", path, null, null), 404);
        assertError(send("PATCH", path, PATCH, "application/json"), 404);
        assertError(send("POST", path, null, null), 404);
        assertEquals(Task.initial(), repository.find(5));
    }

    @Test
    void missingResourceAndDeletionHaveDeterministicStatuses() throws Exception {
        assertError(send("GET", "/api/tasks/999", null, null), 404);
        assertError(send("PATCH", "/api/tasks/999", PATCH, "application/json"), 404);
        assertError(send("DELETE", "/api/tasks/999", null, null), 404);
        HttpResponse<String> deleted = send("DELETE", "/api/tasks/5", null, null);
        assertEquals(204, deleted.statusCode());
        assertEquals("", deleted.body());
        assertNull(repository.find(5));
        HttpResponse<String> missing = send("GET", "/api/tasks/5", null, null);
        assertError(missing, 404);
        assertEquals("no-store", missing.headers().firstValue("Cache-Control").orElseThrow());
        assertError(send("DELETE", "/api/tasks/5", null, null), 404);
        assertError(send("PATCH", "/api/tasks/5", PATCH, "application/json"), 404);
    }

    private HttpResponse<String> send(String method, String path, String body, String contentType)
            throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(base.resolve(path))
                .timeout(Duration.ofSeconds(10));
        if (contentType != null) {
            request.header("Content-Type", contentType);
        }
        request.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private void assertError(HttpResponse<String> response, int status) throws Exception {
        assertEquals(status, response.statusCode(), response.body());
        assertJsonUtf8(response);
        JsonNode error = JSON.readTree(response.body());
        assertTrue(error.isObject());
        assertTrue(error.path("error").isTextual());
        assertFalse(error.path("error").asText().isBlank());
    }

    private void assertJsonUtf8(HttpResponse<String> response) {
        String contentType = response.headers().firstValue("Content-Type").orElseThrow()
                .toLowerCase(Locale.ROOT).replace(" ", "").replace("\"", "");
        assertEquals("application/json;charset=utf-8", contentType);
    }
}
