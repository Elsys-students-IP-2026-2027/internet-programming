package bg.tues.chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;



public class ChatServer implements AutoCloseable {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ServerSocket serverSocket;
    private final Set<PrintWriter> clients = ConcurrentHashMap.newKeySet();
    private final AtomicInteger counter = new AtomicInteger();

    public ChatServer(int port) throws IOException {
        this.serverSocket = new ServerSocket(port);
    }

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 9708;
        ChatServer server = new ChatServer(port);
        System.out.println("Chat server listening on port " + server.getPort());
        server.acceptLoop();
    }

    public int getPort() {
        return serverSocket.getLocalPort();
    }

    public ChatServer start() {
        Thread.ofVirtual().name("chat-acceptor").start(this::acceptLoop);
        return this;
    }

    private void acceptLoop() {
        try {
            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();
                String name = "Client-" + counter.incrementAndGet();
                Thread.ofVirtual().name(name).start(() -> handleClient(socket, name));
            }
        } catch (IOException e) {
            if (!serverSocket.isClosed()) {
                System.err.println("Server error: " + e.getMessage());
            }
        }
    }

    private void handleClient(Socket socket, String name) {
        PrintWriter out = null;
        try (Socket client = socket;
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8))) {
            out = new PrintWriter(new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8), true);
            clients.add(out);
            out.println("Welcome, " + name + "! Type messages. Type 'exit' to leave.");
            broadcast(stamp() + name + " joined the chat.", out);

            String message;
            while ((message = in.readLine()) != null && !message.equalsIgnoreCase("exit")) {
                System.out.println(new ChatMessage(now(), name, message).toJson());
                broadcast(stamp() + name + ": " + message, out);
            }
        } catch (IOException e) {
            System.err.println("Error with " + name + ": " + e.getMessage());
        } finally {
            if (out != null) {
                clients.remove(out);
                broadcast(stamp() + name + " left the chat.", out);
            }
        }
    }

    private void broadcast(String text, PrintWriter sender) {
        for (PrintWriter client : clients) {
            if (client != sender) {
                client.println(text);
            }
        }
    }

    private static String now() {
        return LocalTime.now().format(TIME);
    }

    private static String stamp() {
        return "[" + now() + "] ";
    }

    @Override
    public void close() throws IOException {
        serverSocket.close();
    }
}
