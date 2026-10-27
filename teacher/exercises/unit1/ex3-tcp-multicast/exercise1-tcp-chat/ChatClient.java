

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Client for the broadcast chat.
 * <p>
 * Uses two threads:
 * - the main thread reads from the console and sends to the server;
 * - a virtual thread listens for incoming messages and prints them.
 * This lets the client write and receive messages at the same time.
 */
public class ChatClient {

  private static final String HOST = "localhost";
  private static final int PORT = 9708;

  public static void main(String[] args) {
    try (Socket socket = new Socket(HOST, PORT);
         BufferedReader in = new BufferedReader(
             new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
         PrintWriter out = new PrintWriter(new OutputStreamWriter(
             socket.getOutputStream(), StandardCharsets.UTF_8), true);
         BufferedReader console = new BufferedReader(
             new InputStreamReader(System.in))) {

      System.out.println("Connected to chat server at " + HOST + ":" + PORT);
      System.out.println("Type messages (type 'exit' to quit):");

      Thread receiver = Thread.ofVirtual().start(() -> {
        try {
          String line;
          while ((line = in.readLine()) != null) {
            System.out.println(line);
          }
        } catch (IOException e) {
          System.out.println("Connection closed.");
        }
      });

      String message;
      while ((message = console.readLine()) != null) {
        out.println(message);
        if (message.equalsIgnoreCase("exit")) {
          break;
        }
      }

      socket.close();
      receiver.join();
      System.out.println("Disconnected from server.");
    } catch (ConnectException e) {
      System.out.println("Cannot connect to server at " + HOST + ":" + PORT);
      System.out.println("Make sure ChatServer is running.");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (IOException e) {
      System.err.println("Client error: " + e.getMessage());
    }
  }
}
