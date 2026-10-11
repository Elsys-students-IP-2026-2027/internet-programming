package examples.example4_advanced_chat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class AdvancedChatServer {
    public static final int LISTENING_PORT = 2002;
    public static final String KEEP_ALIVE_MESSAGE = "!keep-alive";
    public static final int CLIENT_READ_TIMEOUT = 5 * 60 * 1000;

    private static ServerSocket mServerSocket;
    private static ServerDispatcher mServerDispatcher;

    public static void main(String[] args) {
        try {
            mServerSocket = new ServerSocket(LISTENING_PORT);
            System.out.println("AdvancedChatServer started on port " + LISTENING_PORT);
            System.out.println("Waiting for clients...\n");

            mServerDispatcher = new ServerDispatcher();
            mServerDispatcher.start();

            int clientCount = 0;
            while (true) {
                Socket socket = mServerSocket.accept();
                clientCount++;
                String clientAddress = socket.getInetAddress().getHostAddress();
                int clientPort = socket.getPort();
                System.out.println("[Client " + clientCount + "] Connected from " + clientAddress + ":" + clientPort);

                Client client = new Client();
                client.mSocket = socket;
                client.mClientId = clientCount;

                try {
                    ClientListener listener = new ClientListener(client, mServerDispatcher);
                    ClientSender sender = new ClientSender(client, mServerDispatcher);

                    client.mClientListener = listener;
                    client.mClientSender = sender;

                    client.mClientListenerThread = Thread.startVirtualThread(listener);
                    client.mClientSenderThread = Thread.startVirtualThread(sender);

                    mServerDispatcher.addClient(client);
                } catch (IOException e) {
                    System.out.println("[Client " + clientCount + "] Error initializing: " + e.getMessage());
                    socket.close();
                }
            }
        } catch (IOException ioe) {
            System.out.println("Server error: " + ioe.getMessage());
        } finally {
            try {
                if (mServerSocket != null && !mServerSocket.isClosed()) {
                    mServerSocket.close();
                }
            } catch (IOException e) {
                System.out.println("Error closing server socket: " + e.getMessage());
            }
        }
    }
}
