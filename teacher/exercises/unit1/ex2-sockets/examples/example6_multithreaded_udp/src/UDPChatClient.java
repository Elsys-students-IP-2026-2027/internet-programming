package examples.example6_multithreaded_udp.src;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;


public class UDPChatClient {

    private static final int SERVER_PORT = 9876;
    private static final String SERVER_HOST = "localhost";

    public static void main(String[] args) {
        try (DatagramSocket clientSocket = new DatagramSocket()) {
            InetAddress serverAddress = InetAddress.getByName(SERVER_HOST);
            System.out.println("UDP chat client. Server at " + SERVER_HOST + ":" + SERVER_PORT);
            System.out.println("Type messages (type 'exit' to quit):");

            Thread receiver = new Thread(() -> {
                byte[] buffer = new byte[1024];
                try {
                    while (true) {
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        clientSocket.receive(packet);
                        String response = new String(packet.getData(), 0, packet.getLength(),
                                StandardCharsets.UTF_8);
                        System.out.println("[Server]: " + response);
                    }
                } catch (Exception e) {
                    // socket is closed — normal end
                  System.out.println("Client stopped.");
                }
            });
            receiver.setDaemon(true);
            receiver.start();

            try (BufferedReader console = new BufferedReader(new InputStreamReader(System.in))) {
                String message;
                while ((message = console.readLine()) != null) {
                    byte[] data = message.getBytes(StandardCharsets.UTF_8);
                    DatagramPacket packet =
                            new DatagramPacket(data, data.length, serverAddress, SERVER_PORT);
                    clientSocket.send(packet);

                    if ("exit".equalsIgnoreCase(message.trim())) {
                        break;
                    }
                }
            }

            System.out.println("Client stopped.");
        } catch (Exception e) {
            System.err.println("Error in client: " + e.getMessage());
        }
    }
}
