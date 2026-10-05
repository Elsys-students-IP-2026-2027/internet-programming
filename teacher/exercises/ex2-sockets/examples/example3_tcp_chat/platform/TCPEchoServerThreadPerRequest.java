package examples.example3_tcp_chat.platform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPEchoServerThreadPerRequest {
    private static final int WORKER_POOL_SIZE = 4;

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            throw new IllegalArgumentException("Parameter(s): <Port>");
        }

        int echoServPort = Integer.parseInt(args[0]);

        // Shared pool of workers that each handle a single request at a time.
        ExecutorService workerPool = Executors.newFixedThreadPool(WORKER_POOL_SIZE);
        Runtime.getRuntime().addShutdownHook(new Thread(workerPool::shutdownNow));

        try (ServerSocket serverSocket = new ServerSocket(echoServPort)) {
            System.out.println("Thread-per-request echo server started on port " + echoServPort
                    + " (worker pool " + WORKER_POOL_SIZE + ")");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Accepted client at " + clientSocket.getRemoteSocketAddress());

                Thread.startVirtualThread(new ConnectionReader(clientSocket, workerPool));
            }
        }
    }

   //Reads one at a time
    private static final class ConnectionReader implements Runnable {
        private final Socket socket;
        private final ExecutorService workerPool;

        private ConnectionReader(Socket socket, ExecutorService workerPool) {
            this.socket = socket;
            this.workerPool = workerPool;
        }

        @Override
        public void run() {
            try (Socket ignored = socket;
                 BufferedReader in = new BufferedReader(
                         new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8)) {

                String request;
                while ((request = in.readLine()) != null) {
                    // Hand this single request to the pool and keep reading.
                    workerPool.execute(new RequestTask(request, out));
                }
            } catch (IOException e) {
                System.err.println("Connection error: " + e.getMessage());
            }
        }
    }

    //when worker is free one request
    private static final class RequestTask implements Runnable {
        private final String request;
        private final PrintWriter out;

        private RequestTask(String request, PrintWriter out) {
            this.request = request;
            this.out = out;
        }

        @Override
        public void run() {
            synchronized (out) {
                out.println("echo: " + request);
            }
        }
    }
}
