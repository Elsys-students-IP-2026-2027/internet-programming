import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Multi-client broadcast chat server.
 * <p>
 * The server accepts many clients at once and broadcasts every received
 * message to ALL connected clients. A separate virtual thread is started for
 * each client, so a slow client does not block the others.
 */
public class ChatServer {

  private static final int PORT = 9708;
  private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

  // Shared list of the output streams of all clients.
  // ConcurrentHashMap.newKeySet() is safe for concurrent access
  private static final Set<PrintWriter> clients = ConcurrentHashMap.newKeySet();

  public static void main(String[] args) {
    System.out.println("Chat server starting on port " + PORT + "...");

    try (ServerSocket serverSocket = new ServerSocket(PORT)) {
      System.out.println("Server is listening. Waiting for clients...");

      int clientCount = 0;
      while (true) {
        Socket socket = serverSocket.accept();
        clientCount++;
        String name = "Client-" + clientCount;
        System.out.println(stamp() + name + " connected: " + socket.getInetAddress().getHostAddress());

        final int id = clientCount;
        Thread.ofVirtual().name(name).start(() -> handleClient(socket, name, id));
      }
    } catch (IOException e) {
      System.err.println("Server error: " + e.getMessage());
    }
  }

  private static void handleClient(Socket socket, String name, int id) {
    PrintWriter out = null;
    try (Socket client = socket;
         BufferedReader in = new BufferedReader(
             new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8))) {

      out = new PrintWriter(new java.io.OutputStreamWriter(
          client.getOutputStream(), StandardCharsets.UTF_8), true);
      clients.add(out);

      out.println("Welcome, " + name + "! Type messages. Type 'exit' to leave.");
      broadcast(stamp() + name + " joined the chat.", out);

      String message;
      while ((message = in.readLine()) != null) {
        if (message.equalsIgnoreCase("exit")) {
          break;
        }
        System.out.println(stamp() + name + ": " + message);

        broadcast(stamp() + name + ": " + message, out);
      }
    } catch (IOException e) {
      System.err.println("Error with " + name + ": " + e.getMessage());
    } finally {
      if (out != null) {
        clients.remove(out);
        broadcast(stamp() + name + " left the chat.", out);
      }
      System.out.println(stamp() + name + " disconnected.");
    }
  }

  private static void broadcast(String text, PrintWriter sender) {
    for (PrintWriter client : clients) {
      if (client != sender) {
        client.println(text);
      }
    }
  }

  private static String stamp() {
    return "[" + LocalTime.now().format(TIME) + "] ";
  }
}
