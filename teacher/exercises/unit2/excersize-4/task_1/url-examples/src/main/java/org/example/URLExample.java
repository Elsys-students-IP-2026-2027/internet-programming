package org.example;

import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

//https://www.w3schools.com/tags/ref_urlencode.ASP
//https://docs.mapp.com/docs/url-encoding-and-what-characters-are-valid-in-a-uri
@Slf4j
public class URLExample {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);
        while (true) {
            System.out.print("Enter a URL (type exit to quit): ");
            if (!scanner.hasNextLine()) {
                break;
            }
            String line = scanner.nextLine().strip();
            if ("exit".equalsIgnoreCase(line)) {
                break;
            }
            if (!line.isEmpty()) {
                analyze(line);
            }
        }
        log.info("Done.");
    }

    private static void analyze(String input) {
        try {
            URI uri = getUriInfo(input);
            // Fails if the authority is not in the form host[:port]
            uri.parseServerAuthority();
            // Convert to URL; throws if no protocol handler exists for the scheme
            URL url = uri.toURL();
            log.info("URL is {}", url);
        } catch (Exception e) {
            log.error("Error ({}): {}", e.getClass().getSimpleName(), e.getMessage());
        }
    }

    private static URI getUriInfo(String uriString) throws URISyntaxException {
        URI myUri = new URI(uriString);
        log.info("Schema is {}", myUri.getScheme());
        log.info("Specific part is {}", myUri.getSchemeSpecificPart());
        log.info("Authority is {}", myUri.getAuthority());
        log.info("Host is {}", myUri.getHost());
        log.info("Port is {}", myUri.getPort());
        log.info("Path is {}", myUri.getPath());
        log.info("Query part {}", myUri.getQuery());
        log.info("Fragment {} ", myUri.getFragment());
        log.info("Opaque: {}, Absolute: {}", myUri.isOpaque(), myUri.isAbsolute());
        return myUri;
    }
}
