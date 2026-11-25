package org.example.upload;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// Parser multipart/form-data
// Content-Type: multipart/form-data; boundary=XyZ
public final class MultipartParser {

    private static final byte[] CRLF = {'\r', '\n'};
    private static final byte[] DASH_DASH = {'-', '-'};
    private static final byte[] HEADERS_END = {'\r', '\n', '\r', '\n'};

    private MultipartParser() {
    }

    public static class MalformedMultipartException extends Exception {
        public MalformedMultipartException(String message) {
            super(message);
        }
    }

    public static String boundary(String contentType) throws MalformedMultipartException {
        String boundary = param(contentType, "boundary");
        if (boundary == null || boundary.isEmpty()) {
            throw new MalformedMultipartException("Липсва boundary в Content-Type");
        }
        if (boundary.length() > 70) {
            throw new MalformedMultipartException("boundary е по-дълъг от 70 знака");
        }
        return boundary;
    }

    public static List<Part> parse(byte[] body, String boundary) throws MalformedMultipartException {
        byte[] first = ("--" + boundary + "\r\n").getBytes(StandardCharsets.US_ASCII);
        byte[] delimiter = ("\r\n--" + boundary).getBytes(StandardCharsets.US_ASCII);

        if (!matchesAt(body, first, 0)) {
            throw new MalformedMultipartException("Тялото не започва с --" + boundary);
        }

        List<Part> parts = new ArrayList<>();
        int position = first.length;
        while (true) {
            int end = indexOf(body, delimiter, position);
            if (end < 0) {
                throw new MalformedMultipartException("Липсва краен разделител --" + boundary + "--");
            }
            parts.add(parsePart(Arrays.copyOfRange(body, position, end)));

            int afterDelimiter = end + delimiter.length;
            if (matchesAt(body, DASH_DASH, afterDelimiter)) {
                return parts;
            }
            if (!matchesAt(body, CRLF, afterDelimiter)) {
                throw new MalformedMultipartException("След разделителя трябва да има CRLF или --");
            }
            position = afterDelimiter + CRLF.length;
        }
    }

    static Part parsePart(byte[] raw) throws MalformedMultipartException {
        int headersEnd = indexOf(raw, HEADERS_END, 0);
        if (headersEnd < 0) {
            throw new MalformedMultipartException("Частта няма празен ред между хедъри и съдържание");
        }
        Map<String, String> headers = parseHeaders(new String(raw, 0, headersEnd, StandardCharsets.UTF_8));
        byte[] data = Arrays.copyOfRange(raw, headersEnd + HEADERS_END.length, raw.length);

        String disposition = headers.get("content-disposition");
        if (disposition == null || !MimeTypes.baseType(disposition).equals("form-data")) {
            throw new MalformedMultipartException("Частта няма Content-Disposition: form-data");
        }
        String name = param(disposition, "name");
        if (name == null || name.isEmpty()) {
            throw new MalformedMultipartException("Частта няма name");
        }
        String filename = param(disposition, "filename");
        String contentType = headers.getOrDefault("content-type", "text/plain");
        return new Part(name, filename, contentType, data);
    }

    static Map<String, String> parseHeaders(String block) throws MalformedMultipartException {
        Map<String, String> headers = new HashMap<>();
        for (String line : block.split("\r\n")) {
            int colon = line.indexOf(':');
            if (colon <= 0) {
                throw new MalformedMultipartException("Грешен хедър в частта: " + line);
            }
            headers.put(line.substring(0, colon).strip().toLowerCase(Locale.ROOT),
                    line.substring(colon + 1).strip());
        }
        return headers;
    }

    static String param(String headerValue, String name) {
        if (headerValue == null) {
            return null;
        }
        int i = headerValue.indexOf(';');
        while (i >= 0 && i < headerValue.length()) {
            i++;
            int eq = headerValue.indexOf('=', i);
            if (eq < 0) {
                return null;
            }
            String key = headerValue.substring(i, eq).strip();
            int j = eq + 1;
            while (j < headerValue.length() && headerValue.charAt(j) == ' ') {
                j++;
            }
            StringBuilder value = new StringBuilder();
            if (j < headerValue.length() && headerValue.charAt(j) == '"') {
                j++;
                while (j < headerValue.length() && headerValue.charAt(j) != '"') {
                    char c = headerValue.charAt(j);
                    if (c == '\\' && j + 1 < headerValue.length()) {
                        c = headerValue.charAt(++j);
                    }
                    value.append(c);
                    j++;
                }
                j = headerValue.indexOf(';', j);
            } else {
                int semicolon = headerValue.indexOf(';', j);
                value.append(headerValue, j, semicolon < 0 ? headerValue.length() : semicolon);
                j = semicolon;
                value = new StringBuilder(value.toString().strip());
            }
            if (key.equalsIgnoreCase(name)) {
                return value.toString();
            }
            i = j;
        }
        return null;
    }

    static int indexOf(byte[] haystack, byte[] needle, int from) {
        outer:
        for (int i = Math.max(from, 0); i <= haystack.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    static boolean matchesAt(byte[] data, byte[] marker, int offset) {
        return offset >= 0 && offset + marker.length <= data.length
                && Arrays.equals(data, offset, offset + marker.length, marker, 0, marker.length);
    }
}

