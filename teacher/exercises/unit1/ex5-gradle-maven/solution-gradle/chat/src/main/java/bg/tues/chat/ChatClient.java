package bg.tues.chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public final class ChatClient {
  private static final String DEFAULT_HOST = "localhost";
  private static final int DEFAULT_PORT = 2002;

  public static void main(String[] args) {
    String host = args.length > 0 ? args[0] : DEFAULT_HOST;
    int port = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_PORT;
    try (Socket socket = new Socket(host, port);
         BufferedReader socketReader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
         PrintWriter socketWriter = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
         BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
      System.out.println("Connected to " + host + ":" + port);
      System.out.println("Commands: /who, /nick <name>, /quit (or EXIT)");

      Thread receiverThread = Thread.startVirtualThread(() -> readFromServer(socketReader));
      Thread senderThread = Thread.startVirtualThread(() -> writeToServer(userInput, socketWriter));
      senderThread.join();
      socket.close();
      receiverThread.join();
    } catch (IOException e) {
      System.out.println("Connection error: " + e.getMessage());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private static void writeToServer(BufferedReader userInput, PrintWriter socketWriter) {
    try {
      String userMessage;
      while ((userMessage = userInput.readLine()) != null) {
        socketWriter.println(userMessage);
        if ("exit".equalsIgnoreCase(userMessage.trim()) || "/quit".equalsIgnoreCase(userMessage.trim())) {
          return;
        }
      }
    } catch (IOException e) {
      System.out.println("Input error: " + e.getMessage());
    }
  }

  private static void readFromServer(BufferedReader socketReader) {
    try {
      String serverMessage;
      while ((serverMessage = socketReader.readLine()) != null) {
        System.out.println(serverMessage);
      }
    } catch (IOException e) {
      if (e.getMessage() == null || !e.getMessage().contains("Socket closed")) {
        System.out.println("Server read error: " + e.getMessage());
      }
    }
  }
}
