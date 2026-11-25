package org.example.upload;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.Executors;

/**
 * Сървър за качване на домашни (multipart/form-data).
 * GET  /                 HTML страница с форма (готово e вече ,не го правите)
 * POST /upload           multipart/form-data: поле "student" + файл "homework"   - ЗАДАЧА 1
 * GET  /files/{name}     сваля качен файл (?download - като прикачен файл)       - ЗАДАЧА 2
 * GET  /info             ехо: протокол и хедъри на заявката (готово)
 * </pre>
 * Стартиране: {@code ./gradlew run} → http://localhost:8080
 */
public class UploadServer {

    public static final int MAX_BODY = 1024 * 1024;

    public static final Set<String> ALLOWED_TYPES = Set.of(
            "application/pdf", "image/png", "image/jpeg", "application/zip", "text/plain");

    private final HttpServer server;
    private final Path uploadDir;

    public UploadServer(int port, Path uploadDir) throws IOException {
        this.uploadDir = Files.createDirectories(uploadDir);
        this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.createContext("/", this::handleIndex);
        server.createContext("/upload", this::handleUpload);
        server.createContext("/files/", this::handleFile);
        server.createContext("/info", this::handleInfo);
    }

    public static void main(String[] args) throws IOException {
        UploadServer app = new UploadServer(8080, Path.of("uploads"));
        app.start();
        System.out.println("Сървърът работи: http://localhost:8080 ");
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
    }

    public int port() {
        return server.getAddress().getPort();
    }

    private void handleIndex(HttpExchange exchange) throws IOException {
        try (exchange) {
            if (!exchange.getRequestURI().getPath().equals("/")) {
                sendText(exchange, 404, "Няма такъв ресурс");
                return;
            }
            try (InputStream in = UploadServer.class.getResourceAsStream("/index.html")) {
                byte[] page = in.readAllBytes();
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, page.length);
                exchange.getResponseBody().write(page);
            }
        }
    }

    private void handleUpload(HttpExchange exchange) throws IOException {
        try (exchange) {
            // TODO 1.1: Само POST - иначе 405 + хедър "Allow: POST".
            //   exchange.getRequestMethod(), exchange.getResponseHeaders().set(...)

            // TODO 1.2: Content-Type трябва да е multipart/form-data - иначе 415.
            //   MimeTypes.baseType(exchange.getRequestHeaders().getFirst("Content-Type"))
            //   Пробвайте формата без enctype – какъв Content-Type изпраща браузърът?

            // TODO 1.3: Вземете boundary с MultipartParser.boundary(contentType).
            //   При MalformedMultipartException - 400 с e.getMessage().

            // TODO 1.4: Прочетете тялото с readBody(exchange). Ако е null - 413 (твърде голямо).

            // TODO 1.5: Разделете тялото на части с MultipartParser.parse(body, boundary).
            //   При MalformedMultipartException - 400.

            // TODO 1.6: Намерете частите с find(parts, "student") и find(parts, "homework").
            //   student: трябва да съществува, да НЕ е файл и text() да не е празен - иначе 400.
            //   homework: трябва да съществува, да е файл и filename() да не е празно → иначе 400.

            // TODO 1.7: Типът се определя от СЪРВЪРА по разширението – не вярваме на клиента:
            //   String type = MimeTypes.forFileName(homework.filename());
            //   Ако не е в ALLOWED_TYPES - 415.

            // TODO 1.8: За двоични файлове (всичко освен text/plain) проверете magic bytes:
            //   MimeTypes.sniff(homework.data()) трябва да е равно на type - иначе 422.
            //   Пробвайте: преименувайте .txt на .pdf и го качете.

            // TODO 1.9: Извикайте saveAndRespond(exchange, student, homework, type, body);
            sendText(exchange, 501, "TODO: имплементирайте handleUpload()");
        }
    }

    private void handleFile(HttpExchange exchange) throws IOException {
        try (exchange) {
            if (!exchange.getRequestMethod().equals("GET")) {
                exchange.getResponseHeaders().set("Allow", "GET");
                sendText(exchange, 405, "Позволен е само GET");
                return;
            }
            String name = exchange.getRequestURI().getPath().substring("/files/".length());
            if (!name.matches("[A-Za-z0-9][A-Za-z0-9._-]*")) {           // без "/", "..", "\" → path traversal
                sendText(exchange, 400, "Невалидно име на файл");
                return;
            }
            Path file = uploadDir.resolve(name);
            if (!Files.isRegularFile(file)) {
                sendText(exchange, 404, "Няма такъв файл");
                return;
            }

            // TODO 2.1: download = true, ако в query-то има параметър "download" (/files/x.pdf?download).
            boolean download = false;

            // TODO 2.2: Вместо DEFAULT вземете типа по име: MimeTypes.forFileName(name).
            //   Какво прави браузърът с PDF, когато типът е application/octet-stream?
            String type = MimeTypes.DEFAULT;

            exchange.getResponseHeaders().set("Content-Disposition",
                    (download ? "attachment" : "inline") + "; filename=\"" + name + "\"");
            exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
            send(exchange, 200, MimeTypes.withCharset(type), Files.readAllBytes(file));
        }
    }

    private void handleInfo(HttpExchange exchange) throws IOException {
        try (exchange) {
            StringBuilder text = new StringBuilder();
            text.append(exchange.getRequestMethod()).append(' ')
                    .append(exchange.getRequestURI()).append(' ')
                    .append(exchange.getProtocol()).append('\n');
            Map<String, List<String>> sorted = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            sorted.putAll(exchange.getRequestHeaders());
            sorted.forEach((name, values) -> values.forEach(v -> text.append(name).append(": ").append(v).append('\n')));
            sendText(exchange, 200, text.toString());
        }
    }

    private void saveAndRespond(HttpExchange exchange, Part student, Part homework, String type, byte[] body)
            throws IOException {
        String stored = UUID.randomUUID().toString().substring(0, 8) + "_" + safeFileName(homework.filename());
        Files.write(uploadDir.resolve(stored), homework.data());

        String json = "{"
                + "\"student\":" + jsonString(student.text().strip()) + ","
                + "\"file\":" + jsonString(stored) + ","
                + "\"declaredType\":" + jsonString(homework.contentType()) + ","
                + "\"detectedType\":" + jsonString(type) + ","
                + "\"size\":" + homework.data().length + ","
                + "\"protocol\":" + jsonString(exchange.getProtocol()) + ","
                + "\"requestContentLength\":" + jsonString(exchange.getRequestHeaders().getFirst("Content-Length")) + ","
                + "\"requestTransferEncoding\":" + jsonString(exchange.getRequestHeaders().getFirst("Transfer-Encoding")) + ","
                + "\"bodyBytes\":" + body.length
                + "}";
        exchange.getResponseHeaders().set("Location", "/files/" + stored);
        send(exchange, 201, "application/json; charset=UTF-8", json.getBytes(StandardCharsets.UTF_8));
    }

    static String safeFileName(String original) {
        String name = original.substring(Math.max(original.lastIndexOf('/'), original.lastIndexOf('\\')) + 1);
        name = name.replaceAll("[^A-Za-z0-9._-]", "_").replaceAll("^[._-]+", "");
        if (name.length() > 100) {
            name = name.substring(name.length() - 100);
        }
        return name.isEmpty() ? "file" : name;
    }

    private static byte[] readBody(HttpExchange exchange) throws IOException {
        String length = exchange.getRequestHeaders().getFirst("Content-Length");
        if (length != null && Long.parseLong(length.strip()) > MAX_BODY) {
            return null;
        }
        byte[] body = exchange.getRequestBody().readNBytes(MAX_BODY + 1);
        return body.length > MAX_BODY ? null : body;
    }

    private static Part find(List<Part> parts, String name) {
        return parts.stream().filter(p -> p.name().equals(name)).findFirst().orElse(null);
    }

    private static String jsonString(String value) {
        if (value == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }

    private static void sendText(HttpExchange exchange, int status, String text) throws IOException {
        send(exchange, status, "text/plain; charset=UTF-8", text.getBytes(StandardCharsets.UTF_8));
    }

    private static void send(HttpExchange exchange, int status, String contentType, byte[] body) throws IOException {
        if (contentType != null) {
            exchange.getResponseHeaders().set("Content-Type", contentType);
        }
        exchange.sendResponseHeaders(status, body.length == 0 ? -1 : body.length);
        exchange.getResponseBody().write(body);
    }
}

