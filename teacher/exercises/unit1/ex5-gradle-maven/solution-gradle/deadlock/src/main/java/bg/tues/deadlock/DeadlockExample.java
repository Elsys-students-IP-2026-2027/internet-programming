package bg.tues.deadlock;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.CompletableFuture;

//inital
public class DeadlockExample {

    public record Result(String serverGot, String clientGot) {
        public boolean deadlocked() {
            return serverGot == null && clientGot == null;
        }
    }

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 5001;
        Result result = run(port, 0);
        System.out.println(result);
    }

    public static Result run(int port, int soTimeoutMs) throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            int actualPort = serverSocket.getLocalPort();
            CompletableFuture<String> server =
                    CompletableFuture.supplyAsync(() -> startServer(serverSocket, soTimeoutMs));
            CompletableFuture<String> client =
                    CompletableFuture.supplyAsync(() -> startClient(actualPort, soTimeoutMs));
            return new Result(server.get(), client.get());
        }
    }

    static String startServer(ServerSocket serverSocket, int soTimeoutMs) {
        try (Socket socket = serverSocket.accept();
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            socket.setSoTimeout(soTimeoutMs);

            System.out.println("Server: waiting for client message...");
            String msg = in.readLine();  // <-- чака клиента
            System.out.println("Server got: " + msg);
            out.println("Hello from server");
            return msg;
        } catch (SocketTimeoutException e) {
            System.out.println("Server: timeout");
            return null;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    static String startClient(int port, int soTimeoutMs) {
        try (Socket socket = new Socket("localhost", port);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            socket.setSoTimeout(soTimeoutMs);

            System.out.println("Client: waiting for server message...");
            String msg = in.readLine();  // <-- чака сървъра
            System.out.println("Client got: " + msg);
            out.println("Hello from client");
            return msg;
        } catch (SocketTimeoutException e) {
            System.out.println("Client: timeout");
            return null;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
