package org.example.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.client.exception.ClientErrorException;
import org.example.client.exception.ServerErrorException;
import org.example.client.exception.ServerUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class UserClient {
    private static final Logger log = LoggerFactory.getLogger(UserClient.class);

    private static final String JSON = "application/json";
    private static final String REQUEST_ID = "X-Request-Id";
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final String baseUrl;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    public UserClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public static String newRequestId() {
        return UUID.randomUUID().toString();
    }

    // ------------------------------------------------------------------ синхронни методи (send)

    public User create(User user) {
        return toUser(send(request("POST", "/users", user, newRequestId())));
    }

    public Optional<User> getById(UUID id) {
        return toOptionalUser(send(request("GET", "/users/" + id, null, newRequestId())));
    }

    public List<User> getAll() {
        HttpResponse<String> response = send(request("GET", "/users", null, newRequestId()));
        ensureSuccess(response);
        return read(response.body(), new TypeReference<>() { });
    }

    /// 404 -> ClientErrorException: there is no user to return.
    public User patch(UUID id, Map<String, Object> fields) {
        return toUser(send(request("PATCH", "/users/" + id, fields, newRequestId())));
    }

    public boolean delete(UUID id) {
        HttpResponse<String> response = send(request("DELETE", "/users/" + id, null, newRequestId()));
        if (response.statusCode() == 404) {
            return false;
        }
        ensureSuccess(response);
        return true;
    }

    // ------------------------------------------------------------------ асинхронни методи (sendAsync)

    public CompletableFuture<User> createAsync(User user) {
        return createAsync(user, newRequestId());
    }

    /// The caller chooses the X-Request-Id, so a retry can send the same id again.
    public CompletableFuture<User> createAsync(User user, String requestId) {
        return sendAsync(request("POST", "/users", user, requestId))
                .thenApply(this::toUser);
    }

    public CompletableFuture<User> patchAsync(UUID id, Map<String, Object> fields) {
        return sendAsync(request("PATCH", "/users/" + id, fields, newRequestId()))
                .thenApply(this::toUser);
    }

    public CompletableFuture<Optional<User>> getByIdAsync(UUID id) {
        return getByIdAsync(id.toString());
    }

    /// Accepts any text, so invalid ids like "abc" reach the server (-> 400).
    public CompletableFuture<Optional<User>> getByIdAsync(String id) {
        return sendAsync(request("GET", "/users/" + id, null, newRequestId()))
                .thenApply(this::toOptionalUser);
    }

    // ------------------------------------------------------------------ общи помощни методи

    private HttpRequest request(String method, String path, Object body, String requestId) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(TIMEOUT)
                .header("Accept", JSON)
                .header(REQUEST_ID, requestId);
        if (body == null) {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            builder.header("Content-Type", JSON)
                    .method(method, HttpRequest.BodyPublishers.ofString(write(body)));
        }
        return builder.build();
    }

    private HttpResponse<String> send(HttpRequest request) {
        long start = System.nanoTime();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            logResponse(request, response.statusCode(), start);
            return response;
        } catch (IOException e) {
            logFailure(request, e, start);
            throw new ServerUnavailableException(requestId(request), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServerUnavailableException(requestId(request), e);
        }
    }

    private CompletableFuture<HttpResponse<String>> sendAsync(HttpRequest request) {
        long start = System.nanoTime();
        return http.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .handle((response, error) -> {
                    if (error != null) {
                        Throwable cause = error instanceof CompletionException && error.getCause() != null
                                ? error.getCause() : error;
                        logFailure(request, cause, start);
                        throw new ServerUnavailableException(requestId(request), cause);
                    }
                    logResponse(request, response.statusCode(), start);
                    return response;
                });
    }

    private User toUser(HttpResponse<String> response) {
        ensureSuccess(response);
        return read(response.body(), new TypeReference<>() { });
    }

    private Optional<User> toOptionalUser(HttpResponse<String> response) {
        if (response.statusCode() == 404) {
            return Optional.empty();
        }
        return Optional.of(toUser(response));
    }

    /// 2xx -> ok, 4xx -> ClientErrorException, 5xx -> ServerErrorException.
    private void ensureSuccess(HttpResponse<String> response) {
        int status = response.statusCode();
        if (status >= 200 && status < 300) {
            return;
        }
        String requestId = requestId(response.request());
        if (status >= 500) {
            throw new ServerErrorException(status, response.body(), requestId);
        }
        throw new ClientErrorException(status, response.body(), requestId);
    }

    private static String requestId(HttpRequest request) {
        return request.headers().firstValue(REQUEST_ID).orElse("-");
    }

    private static void logResponse(HttpRequest request, int status, long startNanos) {
        log.info("{} {}, Status: {}, Duration: {} ms, {}: {}", request.method(), request.uri().getPath(),
                status, millisSince(startNanos), REQUEST_ID, requestId(request));
    }

    private static void logFailure(HttpRequest request, Throwable error, long startNanos) {
        log.warn("{} {}, Failed: {}, Duration: {} ms, {}: {}", request.method(), request.uri().getPath(),
                error.getClass().getSimpleName(), millisSince(startNanos), REQUEST_ID, requestId(request));
    }

    private static long millisSince(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private String write(Object body) {
        try {
            return mapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot serialize request body", e);
        }
    }

    private <T> T read(String body, TypeReference<T> type) {
        try {
            return mapper.readValue(body, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Invalid JSON from server: " + body, e);
        }
    }
}
