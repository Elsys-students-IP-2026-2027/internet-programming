package org.example.utils;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.Reader;

@Slf4j
public class JsonUtils {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static <T> T readJson(Reader reader, Class<T> clazz) {
        try {
            return mapper.readValue(reader, clazz);
        } catch (Exception e) {
            log.warn("Failed to parse JSON: {}", e.getMessage());
            return null;
        }
    }

    public static String toJson(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("Failed to serialize JSON: {}", e.getMessage());
            return "{}";
        }
    }
}
