package examples.example3_tcp_chat.platform;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPEchoServerThreadPerConnection {
    private static final int BUFFER_SIZE = 32;

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            throw new IllegalArgumentException("Parameter(s): <Port>");
        }

        int echoServPort = Integer.parseInt(args[0]);

        try (ServerSocket servSock = new ServerSocket(echoServPort)) {
            System.out.println("Thread-per-connection echo server started on port " + echoServPort);

            while (true) {
                Socket clientSocket = servSock.accept();
                System.out.println("Handling client at " + clientSocket.getRemoteSocketAddress());

                // One new platform (OS) thread per client. Each thread costs round 1 MB of stack, so this does not scale good for many
                // thousands of connections.
                new Thread(new EchoHandler(clientSocket)).start();
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
                int receiveMessageSize;

                while ((receiveMessageSize = in.read(echoBuffer)) != -1) {
                    out.write(echoBuffer, 0, receiveMessageSize);
                    out.flush();
                }
            } catch (IOException e) {
                System.err.println("Client handling exception: " + e.getMessage());
            }
        }
    }
}
