

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPChatClient {

    private static final int SERVER_PORT = 9876;

    public static void main(String[] args) {
        try (DatagramSocket clientSocket = new DatagramSocket()) {
            InetAddress serverAddress = InetAddress.getByName("localhost");
            System.out.println("Connected to UDP chat server at " + serverAddress + ":" + SERVER_PORT);

            // ----- Receiver thread -----
            Thread receiver = new Thread(() -> {
                byte[] receiveBuffer = new byte[1024];
                try {
                    while (true) {
                        DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                        clientSocket.receive(receivePacket);
                        String response = new String(receivePacket.getData(), 0, receivePacket.getLength());
                        System.out.println("\n[Server]: " + response);
                        System.out.print("You: "); // prompt for next input
                    }
                } catch (Exception e) {
                    System.out.println("Receiver thread stopped.");
                }
            });

            receiver.setDaemon(true); // so it ends when the main thread ends
            receiver.start();

            // ----- Sender thread (main thread) -----
            try (BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in))) {
                String message;
                System.out.print("You: ");
                while ((message = userInput.readLine()) != null) {

                    byte[] sendData = message.getBytes();
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, SERVER_PORT);
                    clientSocket.send(sendPacket);

                    if ("quit".equalsIgnoreCase(message.trim())) {
                        System.out.println("Closing client...");
                        break;
                    }

                    System.out.print("You: ");
                }
            }

            System.out.println("Client stopped.");

        } catch (Exception e) {
            System.err.println("Error in client: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
