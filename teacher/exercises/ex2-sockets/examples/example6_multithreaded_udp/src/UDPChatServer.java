package examples.example6_multithreaded_udp.src;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class UDPChatServer {

    private static final int SERVER_PORT = 9876;
    private static final int THREAD_POOL_SIZE = 10;

    public static void main(String[] args) {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        try (DatagramSocket serverSocket = new DatagramSocket(SERVER_PORT)) {
            System.out.println("UDP chat server started on port " + SERVER_PORT);

            while (true) {
                byte[] receiveBuffer = new byte[1024];
                DatagramPacket receivePacket =
                        new DatagramPacket(receiveBuffer, receiveBuffer.length);

                serverSocket.receive(receivePacket);
                executor.submit(() -> handleMessage(receivePacket, serverSocket));
            }
        } catch (Exception e) {
            System.err.println("Could not start server: " + e.getMessage());
        } finally {
            executor.shutdown();
            System.out.println("Server stopped.");
        }
    }

    /** Обработва един получен пакет и връща отговор на подателя. */
    private static void handleMessage(DatagramPacket packet, DatagramSocket socket) {
        try {
            String message = new String(packet.getData(), 0, packet.getLength(),
                    StandardCharsets.UTF_8);
            InetAddress clientAddress = packet.getAddress();
            int clientPort = packet.getPort();

            System.out.printf("Message: '%s' from %s:%d%n", message, clientAddress, clientPort);

            String reply = "exit".equalsIgnoreCase(message.trim())
                    ? "Goodbye from server!"
                    : "Server received: '" + message + "'";

            byte[] sendData = reply.getBytes(StandardCharsets.UTF_8);
            DatagramPacket sendPacket =
                    new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
            socket.send(sendPacket);
        } catch (Exception e) {
            System.err.println("Error processing packet: " + e.getMessage());
        }
    }
}
