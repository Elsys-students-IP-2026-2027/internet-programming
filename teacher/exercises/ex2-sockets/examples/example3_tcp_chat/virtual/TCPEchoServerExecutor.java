package examples.example3_tcp_chat.virtual;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPEchoServerExecutor {
    private static final int BUFFER_SIZE = 32;

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            throw new IllegalArgumentException("Parameter(s): <Port>");
        }

        int echoServPort = Integer.parseInt(args[0]);

        // Each submitted task runs on its own fresh virtual thread, so we get the
        // simple ExecutorService API together with cheap, scalable virtual threads.
        try (ServerSocket servSock = new ServerSocket(echoServPort);
             ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

            System.out.println("Virtual-thread (executor) echo server started on port " + echoServPort);

            while (true) {
                Socket clientSocket = servSock.accept();
                System.out.println("Handling client at " + clientSocket.getRemoteSocketAddress());

                executor.submit(new EchoHandler(clientSocket));
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
