package org.example.upload.httpprobe;

import java.util.ArrayList;
import java.util.List;

/**
 * Parser for the {@code Alt-Svc} header (RFC 7838)
 * <pre>Alt-Svc: h3=":443"; ma=2592000,h3-29=":443"; ma=2592000</pre>
 * With it the server says: "I can also speak another protocol (e.g. HTTP/3 over QUIC)".
 */
public final class AltSvc {

    private AltSvc() {
    }

    public static List<String> protocols(String headerValue) {
        List<String> result = new ArrayList<>();
        if (headerValue == null || headerValue.isBlank() || headerValue.strip().equals("clear")) {
            return result;
        }
        for (String alternative : headerValue.split(",")) {
            int eq = alternative.indexOf('=');
            if (eq > 0) {
                result.add(alternative.substring(0, eq).strip());
            }
        }
        return result;
    }

    public static boolean advertisesHttp3(String headerValue) {
        return protocols(headerValue).stream().anyMatch(p -> p.equals("h3") || p.startsWith("h3-"));
    }
}

