package deadlock;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class DeadlockExample {
    public static void main(String[] args) throws Exception {
        new Thread(() -> startServer()).start();
        Thread.sleep(1000); // wait for the server to start
        startClient();
    }

    static void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(5001);
             Socket socket = serverSocket.accept();
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            System.out.println("Server: waiting for client message...");
            String msg = in.readLine();  // <-- Waits for the client
            System.out.println("Server got: " + msg);
            out.println("Hello from server");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void startClient() {
        try (Socket socket = new Socket("localhost", 5001);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            System.out.println("Client: waiting for server message...");
            String msg = in.readLine();  // <-- Waits for the server
            System.out.println("Client got: " + msg);
            out.println("Hello from client");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
