package org.example;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * In-memory storage with two indexes, so both directions are O(1):
 * long URL -> code (dedup on shorten) and code -> long URL (fast expand).
 */
public class UrlRepository {

    private final ConcurrentHashMap<String, String> codeByUrl = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> urlByCode = new ConcurrentHashMap<>();

    /** Returns the existing code for the URL or atomically creates a new one. */
    public String getOrCreateCode(String longUrl, Supplier<String> codeGenerator) {
        return codeByUrl.computeIfAbsent(longUrl, url -> {
            String code = codeGenerator.get();
            urlByCode.put(code, url);
            return code;
        });
    }

    public Optional<String> findUrlByCode(String code) {
        return Optional.ofNullable(urlByCode.get(code));
    }

    public int size() {
        return urlByCode.size();
    }
}
