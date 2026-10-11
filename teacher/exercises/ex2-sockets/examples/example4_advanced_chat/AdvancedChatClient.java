package examples.example4_advanced_chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class AdvancedChatClient {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 2002;

    public static void main(String[] args) {
        Socket socket = null;
        try {
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            System.out.println("Connected to Chat Server at " + SERVER_HOST + ":" + SERVER_PORT);
            System.out.println("Type messages and press Enter. Type 'EXIT' to quit.\n");

            BufferedReader socketReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter socketWriter = new PrintWriter(socket.getOutputStream(), true);

            Thread receiverThread = Thread.startVirtualThread(new TextDataTransmitter(socketReader, false));
            Thread senderThread = Thread.startVirtualThread(new TextDataTransmitter(socketWriter, true));

            senderThread.join();
            receiverThread.interrupt();

        } catch (java.net.ConnectException e) {
            System.out.println("Cannot connect to server at " + SERVER_HOST + ":" + SERVER_PORT);
            System.out.println("Make sure AdvancedChatServer is running");
        } catch (IOException e) {
            System.out.println("Connection error: " + e.getMessage());
        } catch (InterruptedException e) {
            System.out.println("Connection interrupted");
        } finally {
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                    System.out.println("Disconnected from server");
                }
            } catch (IOException e) {
                System.out.println("Error closing socket: " + e.getMessage());
            }
        }
    }
}

class TextDataTransmitter implements Runnable {
    private BufferedReader mIn;
    private PrintWriter mOut;
    private boolean mIsSender;

    public TextDataTransmitter(BufferedReader in, boolean isSender) {
        mIn = in;
        mOut = null;
        mIsSender = isSender;
    }

    public TextDataTransmitter(PrintWriter out, boolean isSender) {
        mIn = null;
        mOut = out;
        mIsSender = isSender;
    }

    public void run() {
        try {
            if (mIsSender) {
                sendMessages();
            } else {
                receiveMessages();
            }
        } catch (IOException e) {
            if (!Thread.currentThread().isInterrupted()) {
                System.out.println("[Error] " + e.getMessage());
            }
        }
    }

    private void sendMessages() throws IOException {
        BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
        String userMessage;
        while ((userMessage = userInput.readLine()) != null) {
            mOut.println(userMessage);
            mOut.flush();
            if (userMessage.equalsIgnoreCase("EXIT")) {
                break;
            }
        }
    }

    private void receiveMessages() throws IOException {
        String serverMessage;
        while ((serverMessage = mIn.readLine()) != null) {
            if (!serverMessage.equals("!keep-alive")) {
                System.out.println(serverMessage);
            }
        }
    }
}
