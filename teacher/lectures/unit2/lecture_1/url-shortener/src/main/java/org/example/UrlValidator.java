package org.example;

import java.net.URI;

public final class UrlValidator {

    private static final int MAX_LENGTH = 2048;

    private UrlValidator() {
    }

    /** Returns the normalized URL (lowercase scheme and host) or throws IllegalArgumentException. */
    public static String validateAndNormalize(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL must not be empty");
        }
        String trimmed = url.strip();
        if (trimmed.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("URL is longer than " + MAX_LENGTH + " characters");
        }
        URI uri;
        try {
            uri = new URI(trimmed);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid URL syntax: " + e.getMessage());
        }
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new IllegalArgumentException("Only http and https URLs are supported");
        }
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new IllegalArgumentException("URL must contain a host");
        }

        StringBuilder sb = new StringBuilder(scheme.toLowerCase()).append("://");
        if (uri.getRawUserInfo() != null) {
            sb.append(uri.getRawUserInfo()).append('@');
        }
        sb.append(uri.getHost().toLowerCase());
        if (uri.getPort() != -1) {
            sb.append(':').append(uri.getPort());
        }
        sb.append(uri.getRawPath());
        if (uri.getRawQuery() != null) {
            sb.append('?').append(uri.getRawQuery());
        }
        if (uri.getRawFragment() != null) {
            sb.append('#').append(uri.getRawFragment());
        }
        return sb.toString();
    }
}
