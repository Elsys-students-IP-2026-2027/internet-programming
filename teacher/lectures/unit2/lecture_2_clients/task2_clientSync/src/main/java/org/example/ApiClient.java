package org.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.time.Duration;


@Slf4j
public class ApiClient {

    private final URI baseUri;
    private final HttpClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    public ApiClient(String basePath) {
        this.baseUri = URI.create(basePath.endsWith("/") ? basePath : basePath + "/");
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public HttpResponse<String> get(String path)                { return send("GET", path, null); }
    public HttpResponse<String> post(String path, Object body)  { return send("POST", path, body); }
    public HttpResponse<String> put(String path, Object body)   { return send("PUT", path, body); }
    public HttpResponse<String> patch(String path, Object body) { return send("PATCH", path, body); }
    public HttpResponse<String> delete(String path)             { return send("DELETE", path, null); }

    public <T> T get(String path, Class<T> type) {
        HttpResponse<String> resp = get(path);
        try {
            return mapper.readValue(resp.body(), type);
        } catch (JsonProcessingException e) {
            throw new ApiException("Cannot parse JSON from " + path, e);
        }
    }

    private HttpResponse<String> send(String method, String path, Object body) {
        URI uri = resolve(path);
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json");

        BodyPublisher publisher = BodyPublishers.noBody();
        if (body != null) {
            builder.header("Content-Type", "application/json");
            publisher = BodyPublishers.ofString(toJson(body));
        }
        HttpRequest request = builder.method(method, publisher).build();

        try {
            HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("{} {} → {}", method, uri, resp.statusCode());
            log.info("Body: {}", resp.body());
            return resp;
        } catch (IOException e) {
            throw new ApiException(method + " " + uri + " failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();          // restore the interrupt flag
            throw new ApiException(method + " " + uri + " interrupted", e);
        }
    }

    private URI resolve(String path) {
        return baseUri.resolve(path.startsWith("/") ? path.substring(1) : path);
    }

    private String toJson(Object body) {
        if (body instanceof String s) {
            return s;                                    // already JSON
        }
        try {
            return mapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new ApiException("Cannot serialize body", e);
        }
    }

    public static class ApiException extends RuntimeException {
        public ApiException(String msg, Throwable cause) {
            super(msg, cause);
        }
    }
}

