package examples.example1_echo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class EchoClient {

    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS");


    private static void log(String message) {
        System.out.println("[" + LocalTime.now().format(TIME) + "] " + message);
    }

    public static void main(String[] args) {
        String host = "localhost";
        int port = 12345;

        log("Client starting...");

        // Create a client socket and connect to the server.
        // This opens a TCP connection to the host and port where the echo server listens.
        try (Socket socket = new Socket(host, port);
             BufferedReader console = new BufferedReader(
                     new InputStreamReader(System.in));
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            log("Connected to server.");

            // 2. Read one line of input from the user.
            System.out.print("Enter string: ");
            String message = console.readLine();

            // 3. Send the text to the server and wait for the echo.
            // Second client will wait here ,because server will slowly still processing first one
            log("Sending message: " + message);
            out.println(message);

            // 4. Read the echoed response back from the server.
            String echo = in.readLine();
            log("Received echo: " + echo);
        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }
}
