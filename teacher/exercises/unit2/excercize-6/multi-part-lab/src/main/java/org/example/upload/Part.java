package org.example.upload;

import java.nio.charset.StandardCharsets;


public record Part(String name, String filename, String contentType, byte[] data) {

    public boolean isFile() {
        return filename != null;
    }

    public String text() {
        return new String(data, StandardCharsets.UTF_8);
    }
}

