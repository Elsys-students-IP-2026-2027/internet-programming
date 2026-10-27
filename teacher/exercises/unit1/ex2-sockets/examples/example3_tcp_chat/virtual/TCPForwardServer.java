package examples.example3_tcp_chat.virtual;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPForwardServer {
    public static final int SOURCE_PORT = 2525;
    public static final String DESTINATION_HOST = "mail.abv.bg";
    public static final int DESTINATION_PORT = 25;

    public static void main(String[] args) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(SOURCE_PORT)) {
            System.out.println("TCP Forward Server started on port " + SOURCE_PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                Thread.startVirtualThread(new ClientHandler(clientSocket));
            }
        }
    }

    private static final class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private Socket serverSocket;
        private volatile boolean forwardingActive;

        private ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try {
                // Connect to the destination host that will receive forwarded traffic.
                serverSocket = new Socket(DESTINATION_HOST, DESTINATION_PORT);
                serverSocket.setKeepAlive(true);
                clientSocket.setKeepAlive(true);

                InputStream clientIn = clientSocket.getInputStream();
                OutputStream clientOut = clientSocket.getOutputStream();
                InputStream serverIn = serverSocket.getInputStream();
                OutputStream serverOut = serverSocket.getOutputStream();

                forwardingActive = true;
                Thread.startVirtualThread(new ForwardTask(this, clientIn, serverOut));
                Thread.startVirtualThread(new ForwardTask(this, serverIn, clientOut));

                System.out.println("TCP forwarding " + clientSocket.getInetAddress().getHostAddress()
                        + ":" + clientSocket.getPort() + " <--> "
                        + serverSocket.getInetAddress().getHostAddress() + ":"
                        + serverSocket.getPort() + " started.");
            } catch (IOException e) {
                System.err.println("Cannot connect to " + DESTINATION_HOST + ":" + DESTINATION_PORT);
                connectionBroken();
            }
        }

        private synchronized void connectionBroken() {
            try {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                }
            } catch (IOException ignored) {
            }

            try {
                if (!clientSocket.isClosed()) {
                    clientSocket.close();
                }
            } catch (IOException ignored) {
            }

            if (forwardingActive) {
                System.out.println("TCP forwarding stopped.");
                forwardingActive = false;
            }
        }
    }

    private static final class ForwardTask implements Runnable {
        private static final int BUFFER_SIZE = 8192;

        private final ClientHandler parent;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        private ForwardTask(ClientHandler parent, InputStream inputStream, OutputStream outputStream) {
            this.parent = parent;
            this.inputStream = inputStream;
            this.outputStream = outputStream;
        }

        @Override
        public void run() {
            byte[] buffer = new byte[BUFFER_SIZE];

            try {
                while (true) {
                    int bytesRead = inputStream.read(buffer);
                    if (bytesRead == -1) {
                        break;
                    }
                    outputStream.write(buffer, 0, bytesRead);
                    outputStream.flush();
                }
            } catch (IOException ignored) {
                // The peer closed the connection or the network failed.
            } finally {
                parent.connectionBroken();
            }
        }
    }
}
