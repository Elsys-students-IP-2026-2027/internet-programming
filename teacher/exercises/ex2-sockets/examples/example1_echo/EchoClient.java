package examples.example1_echo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class EchoClient {
    public static void main(String[] args) {
        String host = "localhost";
        int port = 12345;
        System.out.println("Hello World. I am Client!");

        // 1. Create a client socket and connect to the server.
        // This opens a TCP connection to the host and port where the echo server listens.
        try (Socket socket = new Socket(host, port);
             BufferedReader console = new BufferedReader(
                     new InputStreamReader(System.in));
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            // 2. Read one line of input from the user.
            System.out.print("Enter string: ");
            String message = console.readLine();

            // 3. Send the text to the server.
            out.println(message);

            // 4. Read the echoed response back from the server.
            System.out.println("Server echo says: " + in.readLine());
        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }
}
