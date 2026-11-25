package org.example.upload;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

import static java.util.Map.entry;

/**
 * MIME types.
 * The MIME type tells the receiver how to interpret the bytes:
 * {@code text/html} is rendered, {@code image/png} is shown as an image,
 * {@code application/octet-stream} means "unknown bytes" – the browser downloads them.
 */
public final class MimeTypes {

    public static final String DEFAULT = "application/octet-stream";

    private static final Map<String, String> BY_EXTENSION = Map.ofEntries(
            entry("html", "text/html"),
            entry("htm", "text/html"),
            entry("css", "text/css"),
            entry("js", "text/javascript"),
            entry("txt", "text/plain"),
            entry("csv", "text/csv"),
            entry("json", "application/json"),
            entry("pdf", "application/pdf"),
            entry("zip", "application/zip"),
            entry("png", "image/png"),
            entry("jpg", "image/jpeg"),
            entry("jpeg", "image/jpeg"),
            entry("gif", "image/gif"),
            entry("svg", "image/svg+xml")
    );

    private MimeTypes() {
    }

    //MIME type by file name
    public static String forFileName(String fileName) {
        if (fileName == null) {
            return DEFAULT;
        }
        int dot = fileName.lastIndexOf('.');
        if (dot <= 0 || dot == fileName.length() - 1) {
            return DEFAULT;
        }
        String extension = fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
        return BY_EXTENSION.getOrDefault(extension, DEFAULT);
    }

    public static String baseType(String contentType) {
        if (contentType == null) {
            return "";
        }
        int semicolon = contentType.indexOf(';');
        String base = semicolon >= 0 ? contentType.substring(0, semicolon) : contentType;
        return base.strip().toLowerCase(Locale.ROOT);
    }

    public static String withCharset(String mimeType) {
        boolean text = mimeType.startsWith("text/")
                || mimeType.equals("application/json")
                || mimeType.equals("image/svg+xml");
        return text ? mimeType + "; charset=UTF-8" : mimeType;
    }

    /**
     * Detection by content.
     * PNG   89 50 4E 47 0D 0A 1A 0A  - image/png
     * JPEG  FF D8 FF                 - image/jpeg
     * GIF   "GIF87a" / "GIF89a"      - image/gif
     * PDF   "%PDF-"                  - application/pdf
     * ZIP   "PK" 03 04               - application/zip
     * If nothing matches (e.g. plain text) - {@code null}.
     */
    public static String sniff(byte[] data) {
        if (startsWith(data, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A)) {
            return "image/png";
        }
        if (startsWith(data, 0xFF, 0xD8, 0xFF)) {
            return "image/jpeg";
        }
        if (startsWith(data, 'G', 'I', 'F', '8', '7', 'a') || startsWith(data, 'G', 'I', 'F', '8', '9', 'a')) {
            return "image/gif";
        }
        if (startsWith(data, '%', 'P', 'D', 'F', '-')) {
            return "application/pdf";
        }
        if (startsWith(data, 'P', 'K', 0x03, 0x04)) {
            return "application/zip";
        }
        return null;
    }

    static boolean startsWith(byte[] data, int... prefix) {
        if (data == null || data.length < prefix.length) {
            return false;
        }
        byte[] expected = new byte[prefix.length];
        for (int i = 0; i < prefix.length; i++) {
            expected[i] = (byte) prefix[i];
        }
        return Arrays.equals(data, 0, prefix.length, expected, 0, prefix.length);
    }
}

