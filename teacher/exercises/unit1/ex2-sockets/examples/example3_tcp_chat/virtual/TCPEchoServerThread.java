package examples.example3_tcp_chat.virtual;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPEchoServerThread {
    private static final int BUFFER_SIZE = 32;

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            throw new IllegalArgumentException("Parameter(s): <Port>");
        }

        int echoServPort = Integer.parseInt(args[0]);

        try (ServerSocket servSock = new ServerSocket(echoServPort)) {
            System.out.println("Virtual-thread echo server started on port " + echoServPort);

            while (true) {
                Socket clntSock = servSock.accept();
                System.out.println("Handling client at " + clntSock.getRemoteSocketAddress());

                Thread.startVirtualThread(new EchoHandler(clntSock));
            }
        }
    }

    private static final class EchoHandler implements Runnable {
        private final Socket clntSock;

        private EchoHandler(Socket clntSock) {
            this.clntSock = clntSock;
        }

        @Override
        public void run() {
            try (Socket ignored = clntSock;
                 InputStream in = clntSock.getInputStream();
                 OutputStream out = clntSock.getOutputStream()) {
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
