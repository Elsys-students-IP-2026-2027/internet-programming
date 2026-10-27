package bg.tues.deadlock;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;

import bg.tues.deadlock.DeadlockExample.Result;


public class DeadlockFixed {

  public static void main(String[] args) throws Exception {
    int port = args.length > 0 ? Integer.parseInt(args[0]) : 5001;
    System.out.println(run(port));
  }

  public static Result run(int port) throws Exception {
    try (ServerSocket serverSocket = new ServerSocket(port)) {
      int actualPort = serverSocket.getLocalPort();
      CompletableFuture<String> server = CompletableFuture.supplyAsync(() -> startServer(serverSocket));
      CompletableFuture<String> client = CompletableFuture.supplyAsync(() -> startClient(actualPort));
      return new Result(server.get(), client.get());
    }
  }

  static String startServer(ServerSocket serverSocket) {
    try (Socket socket = serverSocket.accept();
         BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
         PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
      out.println("Hello from server");   // write..
      String msg = in.readLine();          // read
      System.out.println("Server got: " + msg);
      return msg;
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  static String startClient(int port) {
    try (Socket socket = new Socket("localhost", port);
         BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
         PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
      String msg = in.readLine();  ///read
      System.out.println("Client got: " + msg);
      out.println("Hello from client"); //write
      return msg;
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
