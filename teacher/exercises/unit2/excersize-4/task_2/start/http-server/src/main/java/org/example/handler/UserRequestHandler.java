package org.example.handler;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.example.model.User;
import org.example.service.UserService;
import org.example.utils.JsonUtils;


import java.io.IOException;
import java.util.UUID;

import static org.example.utils.ResponseUtil.method;
import static org.example.utils.ResponseUtil.respondError;
import static org.example.utils.ResponseUtil.respondJson;
import static org.example.utils.ResponseUtil.respondText;


@Slf4j
public class UserRequestHandler extends AbstractHandler {

    private final UserService userService;

    public UserRequestHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void handle(String target, Request baseRequest,
                       HttpServletRequest request, HttpServletResponse response) {

        baseRequest.setHandled(true);
        log.info(" [{}] {}", request.getMethod(), target);

        try {
            if ("/hello".equals(target) && method(request, "GET")) {
                respondText(response, 200, "Hello, World!");
                return;
            }

            if ("/users".equals(target)) {
                handleUserCollection(request, response);
                return;
            }

            if (target.startsWith("/users/")) {
                String idPart = target.substring("/users/".length());
                handleUserById(idPart, request, response);
                return;
            }

            respondError(response, 404, "Not Found");
        } catch (Exception e) {
            log.error("Internal server error", e);
            respondError(response, 500, "Internal Server Error");
        }
    }

    private void handleUserCollection(HttpServletRequest request, HttpServletResponse response) throws IOException {
        switch (request.getMethod()) {
            case "POST" -> {
                User newUser = JsonUtils.readJson(request.getReader(), User.class);
                respondJson(response, 201, userService.createUser(newUser));
            }
            case "GET" -> respondJson(response, 200, userService.getAllUsers());
            case "PUT" -> respondJson(response, 200, userService.resetUserTemplate());
            default -> respondError(response, 405, "Method Not Allowed");
        }
    }

    private void handleUserById(String idStr, HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            UUID id = UUID.fromString(idStr);
            String method = request.getMethod();

            log.info(" [{}] /users/{}", method, id);

            switch (method) {
                case "GET" -> handleGetUserById(id, response);
                case "PUT" -> handlePutUser(id, request, response);
                // TODO: add PATCH
                // TODO: add DELETE
                default -> respondError(response, 405, "Method Not Allowed");
            }

        } catch (IllegalArgumentException e) {
            log.warn("Invalid UUID format: {}", idStr);
            respondError(response, 400, "Invalid UUID format");
        } catch (Exception e) {
            log.error("Error processing user request: {}", e.getMessage(), e);
            respondError(response, 500, "Internal Server Error");
        }
    }

    private void handleGetUserById(UUID id, HttpServletResponse response) throws IOException {
        userService.getUserById(id)
                .ifPresentOrElse(
                        user -> respondJson(response, 200, user),
                        () -> respondError(response, 404, "User not found")
                );
    }

    private void handlePutUser(UUID id, HttpServletRequest request, HttpServletResponse response) throws IOException {
        User updated = JsonUtils.readJson(request.getReader(), User.class);
        userService.replaceUser(id, updated)
                .ifPresentOrElse(
                        user -> respondJson(response, 200, user),
                        () -> respondError(response, 404, "User not found")
                );
    }
}
