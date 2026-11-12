package org.example.utils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

public final class ResponseUtil {

    private static final String CONTENT_TYPE_JSON = "application/json;charset=utf-8";
    private static final String CONTENT_TYPE_TEXT = "text/plain;charset=utf-8";

    private ResponseUtil() {
        // prevent instantiation
    }

    public static void respondJson(HttpServletResponse response, int status, Object body)  {
        try{
            response.setContentType(CONTENT_TYPE_JSON);
            response.setStatus(status);
            response.getWriter().write(JsonUtils.toJson(body));
        } catch (IOException e) {
            throw new RuntimeException("Failed to write JSON response", e);
        }
    }

    public static void respondText(HttpServletResponse response, int status, String text)  {
        try{
            response.setContentType(CONTENT_TYPE_TEXT);
            response.setStatus(status);
            response.getWriter().write(text);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write text response", e);
        }
    }

    public static void respondError(HttpServletResponse response, int status, String message)  {
        respondJson(response, status, Map.of("error", message));
    }

    public static boolean method(HttpServletRequest request, String method) {
        return method.equalsIgnoreCase(request.getMethod());
    }
}
