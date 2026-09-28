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
        try (Socket socket = new Socket(host, port);
             BufferedReader console = new BufferedReader(
                     new InputStreamReader(System.in));
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            System.out.print("Enter string: ");
            String message = console.readLine();
            out.println(message);
            System.out.println("Server echo says: " + in.readLine());
        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }
}
