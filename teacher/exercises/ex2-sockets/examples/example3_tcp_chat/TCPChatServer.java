package examples.example3_tcp_chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TCPChatServer {
    private static final int PORT = 5555;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Chat Server started on port " + PORT);
            System.out.println("Waiting for clients...\n");

            int clientCount = 0;
            while (true) {
                Socket clientSocket = serverSocket.accept();
                clientCount++;
                System.out.println("[" + LocalDateTime.now().format(FORMATTER) + "] Client " + clientCount
                        + " connected: " + clientSocket.getInetAddress().getHostAddress());

                // Run one virtual thread per client so the server can scale to many connections.
                Thread.startVirtualThread(new ClientHandler(clientSocket, clientCount));
            }
        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }

    static class ClientHandler implements Runnable {
        private final Socket socket;
        private final int clientId;

        public ClientHandler(Socket socket, int clientId) {
            this.socket = socket;
            this.clientId = clientId;
        }

        @Override
        public void run() {
            try (Socket ignored = socket;
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                out.println("Welcome to Chat! You are Client " + clientId);
                out.println("Type messages and press Enter. Type 'EXIT' to leave.");

                String message;
                while ((message = in.readLine()) != null) {
                    if (message.equalsIgnoreCase("EXIT")) {
                        break;
                    }
                    System.out.println("[" + LocalDateTime.now().format(FORMATTER) + "] Client " + clientId + ": " + message);
                    out.println("Server received: " + message);
                }

                System.out.println("[" + LocalDateTime.now().format(FORMATTER) + "] Client " + clientId + " disconnected");
            } catch (IOException e) {
                System.out.println("Client " + clientId + " error: " + e.getMessage());
            }
        }
    }
}
