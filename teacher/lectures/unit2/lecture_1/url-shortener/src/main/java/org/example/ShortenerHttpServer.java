package org.example;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;


public class ShortenerHttpServer {

    private final HttpServer server;

    public ShortenerHttpServer(int port, UrlShortenerService service) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", exchange -> handle(exchange, service));
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
    }

    private void handle(HttpExchange exchange, UrlShortenerService service) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();

            if ("/shorten".equals(path)) {
                if (!"POST".equals(method)) {
                    exchange.getResponseHeaders().set("Allow", "POST");
                    send(exchange, 405, "Method Not Allowed");
                    return;
                }
                String body = new String(exchange.getRequestBody().readNBytes(4096), StandardCharsets.UTF_8);
                send(exchange, 201, service.shorten(body));
                return;
            }

            if (!"GET".equals(method)) {
                exchange.getResponseHeaders().set("Allow", "GET");
                send(exchange, 405, "Method Not Allowed");
                return;
            }
            service.expand(path).ifPresentOrElse(
                    url -> redirect(exchange, url),
                    () -> sendQuietly(exchange, 404, "Short URL not found"));
        } catch (IllegalArgumentException e) {
            send(exchange, 400, e.getMessage());
        } catch (Exception e) {
            send(exchange, 500, "Internal Server Error");
        }
    }

    private static void redirect(HttpExchange exchange, String url) {
        try {
            exchange.getResponseHeaders().set("Location", url);
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void sendQuietly(HttpExchange exchange, int status, String text) {
        try {
            send(exchange, status, text);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void send(HttpExchange exchange, int status, String text) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
