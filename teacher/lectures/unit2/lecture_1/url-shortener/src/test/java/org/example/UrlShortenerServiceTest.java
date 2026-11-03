package org.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UrlShortenerServiceTest {

    private static final String LONG_URL = "https://www.example.com/articles/how-to-cook-perfect-pasta-with-sauce";

    private final UrlShortenerService service = new UrlShortenerService();

    @Test
    void shortenProducesShortLink() {
        String shortUrl = service.shorten(LONG_URL);
        assertTrue(shortUrl.startsWith("https://short.ly/"));
        assertTrue(shortUrl.length() < LONG_URL.length());
    }

    @Test
    void expandReturnsOriginal() {
        String shortUrl = service.shorten(LONG_URL);
        assertEquals(LONG_URL, service.expand(shortUrl).orElseThrow());
        assertEquals(LONG_URL, service.expand(shortUrl.substring(shortUrl.lastIndexOf('/') + 1)).orElseThrow());
    }

    @Test
    void sameUrlGetsSameShortUrl() {
        assertEquals(service.shorten(LONG_URL), service.shorten(LONG_URL));
        assertEquals(service.shorten(LONG_URL), service.shorten("HTTPS://WWW.EXAMPLE.COM/articles/how-to-cook-perfect-pasta-with-sauce"));
    }

    @Test
    void differentUrlsGetDifferentShortUrls() {
        assertNotEquals(service.shorten("https://a.com/1"), service.shorten("https://a.com/2"));
        assertNotEquals(service.shorten("https://a.com/x?q=1"), service.shorten("https://a.com/x?q=2"));
    }

    @Test
    void unknownCodeIsEmpty() {
        assertTrue(service.expand("https://short.ly/zzzzz").isEmpty());
        assertTrue(service.expand("").isEmpty());
        assertTrue(service.expand(null).isEmpty());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "example.com", "ftp://example.com/file", "mailto:a@b.com", "https://", "https://exa mple.com"})
    void invalidUrlsAreRejected(String url) {
        assertThrows(IllegalArgumentException.class, () -> service.shorten(url));
    }

    @Test
    void concurrentShorteningStaysUnique() throws Exception {
        int threads = 8, perThread = 2000;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        Set<String> codes = ConcurrentHashMap.newKeySet();
        for (int t = 0; t < threads; t++) {
            int offset = t * perThread;
            pool.submit(() -> IntStream.range(0, perThread)
                    .forEach(i -> codes.add(service.shorten("https://example.com/page/" + (offset + i)))));
        }
        pool.shutdown();
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS));
        assertEquals(threads * perThread, codes.size());
    }

    @Test
    void concurrentShorteningOfSameUrlGivesOneCode() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(8);
        Set<String> codes = ConcurrentHashMap.newKeySet();
        for (int i = 0; i < 1000; i++) {
            pool.submit(() -> codes.add(service.shorten(LONG_URL)));
        }
        pool.shutdown();
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS));
        assertEquals(1, codes.size());
    }

    @Test
    void base62RoundTrip() {
        for (long n : new long[]{0, 1, 61, 62, 14_776_336L, Long.MAX_VALUE}) {
            assertEquals(n, Base62.decode(Base62.encode(n)));
        }
    }
}
