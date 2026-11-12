package org.example;

import org.eclipse.jetty.server.Server;
import org.example.handler.TaskHandler;
import org.example.repository.TaskRepository;

public class HttpServerApp {
    public static void main(String[] args) throws Exception {
        int port = args.length == 0 ? 8000 : Integer.parseInt(args[0]);
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("Port must be between 0 and 65535");
        }
        Server server = new Server(port);
        server.setHandler(new TaskHandler(new TaskRepository()));
        server.setStopAtShutdown(true);
        server.start();
        server.join();
    }
}
