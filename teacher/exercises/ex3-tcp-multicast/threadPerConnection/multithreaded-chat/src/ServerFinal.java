

import java.io.IOException;
import java.io.InputStreamReader;



import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerFinal {

    public static void main(String[] args) {
        System.out.println("Hello World. I am Server!");
        try (ServerSocket serverSocket = new ServerSocket(9708)) {
            System.out.println("Server is listening on port 9708...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress());

                // Handle each client in a new thread
                new Thread(new ClientHandler(clientSocket)).start();
            }

        } catch (IOException e) {
            System.out.println("Error while starting the server: " + e.getMessage());
        }
    }

    // Runnable class to handle client communication
    private static class ClientHandler implements Runnable {
        private final Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

                String receivedMessage;
                while ((receivedMessage = in.readLine()) != null) {
                    System.out.println("Received message from client: " + receivedMessage);
                    out.println("Server response: " + receivedMessage);

                    if (receivedMessage.equalsIgnoreCase("exit")) {
                        out.println("Exiting connection...");
                        System.out.println("Client requested disconnection.");
                        break;
                    }
                }

            } catch (IOException e) {
                System.out.println("Error while processing client request: " + e.getMessage());
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    System.out.println("Error closing client socket: " + e.getMessage());
                }
            }
        }
    }
}
