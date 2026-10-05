package examples.example1_echo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class EchoServer {

  private static final DateTimeFormatter TIME =
      DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

  private static void log(String message) {
    System.out.println("[" + LocalTime.now().format(TIME) + "] " + message);
  }

  static void main(String[] args) {
    int port = args.length > 0 ? Integer.parseInt(args[0]) : 12345;
    int workSeconds = args.length > 1 ? Integer.parseInt(args[1]) : 15;
    log("Hello World. I am Server!");

    // binding and listening for TCP connections.
    try (ServerSocket server = new ServerSocket(port)) {
      while (true) {
        // accept() blocking wait 3 ways handshake.
        log("Waiting for client...");
        Socket socket = server.accept();
        log("Connection established: " + socket.getInetAddress());

        // InputStream -reading,
        // а OutputStream -writing.
        try (BufferedReader in = new BufferedReader(
            new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

          // readLine() return null, when client disconnects.
          String line;
          while ((line = in.readLine()) != null) {
            log("Received: " + line);

            // Add slow processing.Simulation for long-running tasks for particular client.
            // The server is processing only one client at a time,the rest of the clients are waiting.
            log("Processing slowly (" + workSeconds + "s)...");
            for (int i = workSeconds; i > 0; i--) {
              log("  working... " + i);
              try {
                Thread.sleep(1000);
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
              }
            }
            log("Done. Sending echo back.");

            // send echo back to client.
            out.println(line);
          }
        } catch (IOException e) {
          System.err.println("Client error: " + e.getMessage());
        }

        // try-with-resources close socket automatically.
        log("Client disconnected. Ready for the next one.");
      }
    } catch (IOException e) {
      System.err.println("Server error: " + e.getMessage());
    }
  }
}
