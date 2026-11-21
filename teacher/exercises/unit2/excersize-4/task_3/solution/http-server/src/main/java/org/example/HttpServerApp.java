package org.example;

import org.eclipse.jetty.server.Server;
import org.example.handler.TaskHandler;
import org.example.repository.TaskRepository;

public final class HttpServerApp {
    public static void main(String[] args) throws Exception {
        int port = args.length == 0 ? 8000 : Integer.parseInt(args[0]);
        Server server = new Server(port);
        server.setHandler(new TaskHandler(new TaskRepository()));
        server.setStopAtShutdown(true);
        server.start();
        System.out.println("Задачи: http://localhost:" + port + "/api/tasks/5");
        server.join();
    }
}
