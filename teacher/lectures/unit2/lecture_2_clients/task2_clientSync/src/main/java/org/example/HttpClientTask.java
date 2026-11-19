package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class HttpClientTask {

    private static final Logger logger = LoggerFactory.getLogger(HttpClientTask.class);
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) throws IOException, InterruptedException {

        sendGet("http://localhost:8000/hello");


        String newUserJson = """
                {
                  "name": "Alice",
                  "age": 25,
                  "email": "alice@example.com",
                  "status": "processing"
                }
                """;
        sendPost("http://localhost:8000/user", newUserJson);

        String patchJson = """
                {
                  "status": "active"
                }
                """;
    }

    public static void sendGet(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            logger.info("GET {} → Status: {}", url, response.statusCode());
            logger.info("Response body: {}", response.body());

        } catch (IOException | InterruptedException e) {
            logger.error("GET request failed: {}", e.getMessage());
        }
    }

    public static void sendPost(String url, String json) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            logger.info("POST {} → Status: {}", url, response.statusCode());
            logger.info("Response body: {}", response.body());

        } catch (IOException | InterruptedException e) {
            logger.error("POST request failed: {}", e.getMessage());
        }
    }

    public static void sendPut(String url, String json) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            logger.info("PUT {} → Status: {}", url, response.statusCode());
            logger.info("Response body: {}", response.body());

        } catch (IOException | InterruptedException e) {
            logger.error("PUT request failed: {}", e.getMessage());
        }
    }

    public static void sendPatch(String url, String json) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            logger.info("PATCH {} → Status: {}", url, response.statusCode());
            logger.info("Response body: {}", response.body());

        } catch (IOException | InterruptedException e) {
            logger.error("PATCH request failed: {}", e.getMessage());
        }
    }

    public static void sendDelete(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .DELETE()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            logger.info("DELETE {} → Status: {}", url, response.statusCode());
            logger.info("Response body: {}", response.body());

        } catch (IOException | InterruptedException e) {
            logger.error("DELETE request failed: {}", e.getMessage());
        }
    }
}

