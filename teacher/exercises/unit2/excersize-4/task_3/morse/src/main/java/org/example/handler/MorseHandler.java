package org.example.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.example.service.MorseService;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class MorseHandler extends AbstractHandler {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String SECRET_MORSE =
            "-. --- / -.- .- -.- .-- --- / . / --- -.- . .- -. --.-- - --..--"
                    + " / .- -.- --- / -. . / ... --.-- .-- -.- ..- .--. -. --- ... -"
                    + " / --- - / -.- .- .--. -.- .. ..--..";
    private final MorseService service = new MorseService();

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request,
                       HttpServletResponse response) throws IOException {
        baseRequest.setHandled(true);
        boolean conversion = "/api/encode".equals(target) || "/api/decode".equals(target);
        if (!conversion && !"/api/secret-message".equals(target) && !"/api/hint".equals(target)) {
            error(response, 404, "Not Found");
            return;
        }

        String allowedMethod = conversion ? "POST" : "GET";
        if (!allowedMethod.equals(request.getMethod())) {
            response.setHeader("Allow", allowedMethod);
            error(response, 405, "Method Not Allowed");
            return;
        }
        if ("/api/secret-message".equals(target)) {
            text(response, SECRET_MORSE);
            return;
        }
        if ("/api/hint".equals(target)) {
            text(response, "Две следи — къса (.) и дълга (-) — носят буквите. "
                    + "Пауза от един интервал дели буквите, а ' / ' дели думите. "
                    + "Потърси българската морзова азбука — отговорът е на кирилица.");
            return;
        }

        String contentType = request.getContentType();
        if (contentType == null || !"text/plain".equalsIgnoreCase(contentType.split(";", 2)[0].trim())) {
            error(response, 415, "Content-Type must be text/plain; request text is UTF-8");
            return;
        }
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        StringWriter body = new StringWriter();
        request.getReader().transferTo(body);
        String result;
        try {
            result = "/api/encode".equals(target)
                    ? service.encode(body.toString()) : service.decode(body.toString());
        } catch (IllegalArgumentException e) {
            error(response, 400, e.getMessage());
            return;
        }
        text(response, result);
    }

    private static void text(HttpServletResponse response, String body) throws IOException {
        response.setStatus(200);
        response.setContentType("text/plain;charset=utf-8");
        response.getWriter().write(body);
    }

    private static void error(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=utf-8");
        response.getWriter().write(JSON.writeValueAsString(Map.of("error", message)));
    }
}
