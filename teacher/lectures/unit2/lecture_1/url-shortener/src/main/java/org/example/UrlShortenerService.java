package org.example;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 1. Shorten URL  - shorten(longUrl)
 * 2. Expand URL   - expand(shortUrlOrCode)
 * 3. Performance  - hash-map lookups, O(1) in both directions
 * 4. Uniqueness   - a unique counter value is encoded in Base62, so two different
 *                   long URLs can never get the same code (no hash collisions);
 *                   the same long URL always gets the same code.
 */
public class UrlShortenerService {

    public static final String DEFAULT_BASE_URL = "https://short.ly/";
    // 62^4: the first generated code has 5 characters
    private static final long FIRST_ID = 14_776_336L;

    private final UrlRepository repository;
    private final String baseUrl;
    private final AtomicLong idGenerator = new AtomicLong(FIRST_ID);

    public UrlShortenerService(UrlRepository repository, String baseUrl) {
        this.repository = repository;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
    }

    public UrlShortenerService() {
        this(new UrlRepository(), DEFAULT_BASE_URL);
    }

    public String shorten(String longUrl) {
        String normalized = UrlValidator.validateAndNormalize(longUrl);
        String code = repository.getOrCreateCode(normalized, () -> Base62.encode(idGenerator.getAndIncrement()));
        return baseUrl + code;
    }

    /** Accepts either the full short URL or only the code. */
    public Optional<String> expand(String shortUrlOrCode) {
        if (shortUrlOrCode == null || shortUrlOrCode.isBlank()) {
            return Optional.empty();
        }
        String value = shortUrlOrCode.strip();
        String code = value.substring(value.lastIndexOf('/') + 1);
        return repository.findUrlByCode(code);
    }
}
