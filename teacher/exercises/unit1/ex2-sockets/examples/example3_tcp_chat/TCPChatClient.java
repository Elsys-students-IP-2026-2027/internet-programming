package examples.example3_tcp_chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;

public class TCPChatClient {
    public static void main(String[] args) {
        String host = "localhost";
        int port = 5555;

        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in))) {

            System.out.println("Connected to Chat Server");

           ///
            Thread receiverThread = Thread.startVirtualThread(() -> {
                try {
                    String serverMessage;
                    while ((serverMessage = in.readLine()) != null) {
                        System.out.println(serverMessage);
                    }
                } catch (IOException e) {
                    System.out.println("Connection closed");
                }
            });

            String userMessage;
            while ((userMessage = userInput.readLine()) != null) {
                out.println(userMessage);
                if (userMessage.equalsIgnoreCase("EXIT")) {
                    break;
                }
            }

            receiverThread.join();
            System.out.println("Disconnected from server");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Client interrupted");
        } catch (ConnectException e) {
            System.out.println("Cannot connect to server at " + host + ":" + port);
            System.out.println("Make sure TCPChatServer is running");
        } catch (IOException e) {
            System.out.println("Client error: " + e.getMessage());
        }
    }
}
