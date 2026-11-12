package org.example;

import org.eclipse.jetty.server.Server;
import org.example.handler.MorseHandler;

public class HttpServerApp {
    public static void main(String[] args) throws Exception {
        int port = args.length == 0 ? 8000 : Integer.parseInt(args[0]);
        Server server = new Server(port);
        server.setHandler(new MorseHandler());
        server.setStopAtShutdown(true);
        server.start();
        server.join();
    }
}
