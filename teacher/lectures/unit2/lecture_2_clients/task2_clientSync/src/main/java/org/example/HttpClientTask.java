package org.example;
import java.util.Map;

public class HttpClientTask {
    public static void main(String[] args) {

        ApiClient local = new ApiClient("http://localhost:8000");
        try {
            local.get("/hello");
            local.post("/user", Map.of(
                    "name", "Alice",
                    "age", 25,
                    "email", "alice@example.com",
                    "status", "processing"));
            local.patch("/user/1", Map.of("status", "active"));
            local.delete("/user/1");
        } catch (ApiClient.ApiException e) {
            System.err.println("Local server is not responding: " + e.getMessage());
        }

        ApiClient placeholder = new ApiClient("https://jsonplaceholder.typicode.com");
        Post post = placeholder.get("/posts/1", Post.class);
        System.out.println("Deserialized: " + post);
        placeholder.post("/posts", new Post(1, 0, "Title", "Body"));
    }
}
