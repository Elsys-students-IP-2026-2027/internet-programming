package org.example.upload.httpprobe;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * Which HTTP version is actually used?
 * request.version(HTTP_2) is only a PREFERENCE. The real version is negotiated
 * with the server (over HTTPS – via ALPN during the TLS handshake) and is visible in response.version().
 * Run: ./gradlew probe
 */
public class HttpProbe {

    static final List<String> DEFAULT_URLS = List.of(
            "https://www.google.com/",
            "https://www.cloudflare.com/",
            "https://www.wikipedia.org/",
            "https://elsys-bg.org/",
            "https://example.com/",
            "https://github.com/",
            "http://localhost:8080/info"          // your UploadServer – start it first
    );

    /**
     * Result of a single request.
     *
     * @param preferred the version we asked for
     * @param actual    the version actually used ({@code response.version()})
     * @param status    status code
     * @param altSvc    value of the {@code Alt-Svc} header or {@code null}
     * @param millis    time until the whole response was received
     */
    public record ProbeResult(URI uri, HttpClient.Version preferred, HttpClient.Version actual,
                              int status, String altSvc, long millis) {
    }

    public static ProbeResult probe(HttpClient client, URI uri, HttpClient.Version preferred)
            throws IOException, InterruptedException {
        // the version is only a request, not mandatory
        HttpRequest request = HttpRequest.newBuilder(uri)
                .version(preferred)
                .timeout(Duration.ofSeconds(10))
                .header("User-Agent", "HttpProbe/1.0 (school exercise)")   // some sites return 403 without it
                .GET()
                .build();

        // send and time the request
        long start = System.nanoTime();
        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        long millis = (System.nanoTime() - start) / 1_000_000;

        // the real version comes from the response
        return new ProbeResult(uri, preferred, response.version(), response.statusCode(),
                response.headers().firstValue("alt-svc").orElse(null), millis);
    }

    public static void main(String[] args) throws InterruptedException {
        List<String> urls = args.length > 0 ? List.of(args) : DEFAULT_URLS;
        System.out.printf("%-30s %-9s %-9s %6s %7s  %s%n",
                "URL", "Preferred", "Actual", "Status", "ms", "HTTP/3 advertised?");

        for (String url : urls) {
            for (HttpClient.Version preferred : List.of(HttpClient.Version.HTTP_1_1, HttpClient.Version.HTTP_2)) {
                // new client -> new TCP/TLS connection
                try (HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(5))
                        .followRedirects(HttpClient.Redirect.NEVER)   // we want to see 301/302, not the final site
                        .build()) {
                    ProbeResult r = probe(client, URI.create(url), preferred);
                    System.out.printf("%-30s %-9s %-9s %6d %7d  %s%n",
                            shorten(url), name(r.preferred()), name(r.actual()), r.status(), r.millis(),
                            AltSvc.advertisesHttp3(r.altSvc()) ? "yes: " + r.altSvc() : "no");
                } catch (IOException e) {
                    System.out.printf("%-30s %-9s ERROR: %s%n", shorten(url), name(preferred), e);
                }
            }
        }
    }

    private static String name(HttpClient.Version version) {
        return switch (version) {
            case HTTP_1_1 -> "HTTP/1.1";
            case HTTP_2 -> "HTTP/2";
            default -> version.toString();   // HTTP_3 in JDK 26+
        };
    }

    private static String shorten(String url) {
        return url.length() <= 30 ? url : url.substring(0, 27) + "...";
    }
}

