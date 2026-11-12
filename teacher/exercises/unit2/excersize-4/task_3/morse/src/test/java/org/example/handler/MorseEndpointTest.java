package org.example.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class MorseEndpointTest {
    private final HttpClient client = HttpClient.newHttpClient();
    private Server server;
    private URI base;

    @BeforeEach
    void start() throws Exception {
        server = new Server(0);
        server.setHandler(new MorseHandler());
        server.start();
        base = URI.create("http://localhost:"
                + ((ServerConnector) server.getConnectors()[0]).getLocalPort());
    }

    @AfterEach
    void stop() throws Exception {
        server.stop();
    }

    @Test
    void retrievesAndConvertsTheSecret() throws Exception {
        String encoded = "-. --- / -.- .- -.- .-- --- / . / --- -.- . .- -. --.-- - --..--"
                + " / .- -.- --- / -. . / ... --.-- .-- -.- ..- .--. -. --- ... -"
                + " / --- - / -.- .- .--. -.- .. ..--..";
        assertText(send("GET", "/api/secret-message", null, null), encoded);
        HttpResponse<String> decoded = send("POST", "/api/decode", encoded, "text/plain");
        assertEquals(200, decoded.statusCode());
        assertTrue(decoded.body().matches("[А-Я ,?]+"), "The secret decodes to Bulgarian text");
        for (String type : new String[]{"text/plain", "TEXT/PLAIN; charset=UTF-8",
                "text/plain; charset=ISO-8859-1"}) {
            assertText(send("POST", "/api/encode", decoded.body(), type), encoded);
        }
        HttpResponse<String> hint = send("GET", "/api/hint", null, null);
        assertEquals(200, hint.statusCode());
        assertTrue(hint.body().contains(" / "));
        assertTrue(hint.body().contains("интервал"));
        assertFalse(hint.body().contains(decoded.body()));
        assertFalse(hint.body().contains(encoded));
    }

    @Test
    void rejectsInvalidInputAndContentTypes() throws Exception {
        for (String path : new String[]{"/api/encode", "/api/decode"}) {
            for (String body : new String[]{"", " ", "hello", " .-", "!", "а  б"}) {
                assertError(send("POST", path, body, "text/plain"), 400);
            }
            for (String type : new String[]{null, "application/json", "text/plainly"}) {
                assertError(send("POST", path, "", type), 415);
            }
        }
    }

    @Test
    void enforcesMethodsAndHasNoUnrelatedRoutes() throws Exception {
        for (String path : new String[]{"/api/secret-message", "/api/hint", "/api/encode", "/api/decode"}) {
            String allowed = path.equals("/api/encode") || path.equals("/api/decode") ? "POST" : "GET";
            for (String method : new String[]{"GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"}) {
                if (method.equals(allowed)) {
                    continue;
                }
                HttpResponse<String> response = send(method, path, null, null);
                assertEquals(405, response.statusCode());
                assertEquals(allowed, response.headers().firstValue("Allow").orElseThrow());
                if (method.equals("HEAD")) {
                    assertEquals("", response.body());
                } else {
                    assertError(response, 405);
                }
            }
        }
        for (String path : new String[]{"/users", "/hello", "/secret", "/api/tasks/5",
                "/api/encode/", "/api/decode/extra", "/api/unknown", "/API/hint"}) {
            assertError(send("GET", path, null, null), 404);
            assertError(send("POST", path, "А", "text/plain"), 404);
        }
    }

    private HttpResponse<String> send(String method, String path, String body, String type) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(base.resolve(path));
        if (type != null) {
            request.header("Content-Type", type);
        }
        request.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private void assertText(HttpResponse<String> response, String expected) {
        assertEquals(200, response.statusCode());
        assertContentType(response, "text/plain");
        assertEquals(expected, response.body());
    }

    private void assertError(HttpResponse<String> response, int status) throws Exception {
        assertEquals(status, response.statusCode());
        assertContentType(response, "application/json");
        assertFalse(new ObjectMapper().readTree(response.body()).path("error").asText().isBlank());
    }

    private void assertContentType(HttpResponse<String> response, String expected) {
        String type = response.headers().firstValue("Content-Type").orElseThrow().toLowerCase(Locale.ROOT);
        assertEquals(expected, type.split(";", 2)[0]);
        assertTrue(type.contains("charset=utf-8"));
    }
}
