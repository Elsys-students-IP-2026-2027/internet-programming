package org.example;


import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.Server;
import org.example.repository.UserRepository;
import org.example.service.UserService;
import org.example.handler.UserRequestHandler;

@Slf4j
public class HttpServerApp {
    public static void main(String[] args) throws Exception {
        int port = 8000;
        Server server = new Server(port);
        server.setHandler(new UserRequestHandler(new UserService(new UserRepository())));

        log.info("Starting HTTP Server on port {}", port);
        server.start();
        log.info("HTTP Server started successfully on port {}", port);

        server.join();
    }
}
