package org.example;

import java.nio.charset.StandardCharsets;
import java.util.Scanner;


public class Main {

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && "--server".equals(args[0])) {
            int port = args.length > 1 ? Integer.parseInt(args[1]) : 8080;
            new ShortenerHttpServer(port,
                    new UrlShortenerService(new UrlRepository(), "http://localhost:" + port + "/")).start();
            System.out.println("Server started on http://localhost:" + port);
            return;
        }
        runConsole();
    }

    private static void runConsole() {
        UrlShortenerService service = new UrlShortenerService();
        Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);
        System.out.println("Commands: shorten <long url> | expand <short url> | exit");
        while (true) {
            System.out.print("> ");
            if (!scanner.hasNextLine()) {
                break;
            }
            String line = scanner.nextLine().strip();
            if ("exit".equalsIgnoreCase(line)) {
                break;
            }
            String[] parts = line.split("\\s+", 2);
            try {
                switch (parts[0].toLowerCase()) {
                    case "shorten" -> System.out.println(service.shorten(arg(parts)));
                    case "expand" -> System.out.println(service.expand(arg(parts)).orElse("Short URL not found"));
                    case "" -> { }
                    default -> System.out.println("Unknown command");
                }
            } catch (IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private static String arg(String[] parts) {
        if (parts.length < 2) {
            throw new IllegalArgumentException("Missing argument");
        }
        return parts[1];
    }
}
