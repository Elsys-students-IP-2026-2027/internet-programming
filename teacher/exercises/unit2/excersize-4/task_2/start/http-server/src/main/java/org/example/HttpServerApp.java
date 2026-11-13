package org.example;


import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.Server;
import org.example.repository.UserRepository;
import org.example.service.UserService;
import org.example.handler.UserRequestHandler;

import java.io.IOException;

@Slf4j
public class HttpServerApp {
    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8000;
        Server server = new Server(port);
        server.setHandler(new UserRequestHandler(new UserService(new UserRepository())));
        server.setStopAtShutdown(true);

        log.info("Starting HTTP Server on port {}", port);
        try {
            server.start();
        } catch (IOException e) {
            log.error("Port {} is already in use. Stop the other server or start with another port, "
                    + "e.g. ./gradlew run --args='8001'", port);
            server.stop();
            System.exit(1);
        }
        log.info("HTTP Server started successfully on port {}", port);

        server.join();
    }
}
