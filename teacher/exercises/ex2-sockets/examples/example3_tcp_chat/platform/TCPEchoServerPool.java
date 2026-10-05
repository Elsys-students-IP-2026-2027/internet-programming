package examples.example3_tcp_chat.platform;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPEchoServerPool {
    private static final int BUFFER_SIZE = 32;
    private static final int THREAD_POOL_SIZE = 10;

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            throw new IllegalArgumentException("Parameter(s): <Port>");
        }

        int echoServPort = Integer.parseInt(args[0]);

        // A fixed thread pool reuses a bounded set of platform threads instead of
        // creating a new thread per connection.
        ExecutorService threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        try (ServerSocket serverSocket = new ServerSocket(echoServPort)) {
            System.out.println("Thread-pool echo server started on port " + echoServPort
                    + " (pool size " + THREAD_POOL_SIZE + ")");

            // Shut down the pool
            Runtime.getRuntime().addShutdownHook(new Thread(threadPool::shutdownNow));

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Handling client at " + clientSocket.getRemoteSocketAddress());

                threadPool.execute(new EchoHandler(clientSocket));
            }
        }
    }

    private static final class EchoHandler implements Runnable {
        private final Socket clientSocket;

        private EchoHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try (Socket ignored = clientSocket;
                 InputStream in = clientSocket.getInputStream();
                 OutputStream out = clientSocket.getOutputStream()) {
                byte[] echoBuffer = new byte[BUFFER_SIZE];
                int recvMsgSize;

                while ((recvMsgSize = in.read(echoBuffer)) != -1) {
                    out.write(echoBuffer, 0, recvMsgSize);
                    out.flush();
                }
            } catch (IOException e) {
                System.err.println("Client handling exception: " + e.getMessage());
            }
        }
    }
}
