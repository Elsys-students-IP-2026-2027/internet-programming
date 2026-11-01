

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UDPChatServer {

    private static final int SERVER_PORT = 9876;
    private static final int THREAD_POOL_SIZE = 10; // number of worker threads

    public static void main(String[] args) {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        try (DatagramSocket serverSocket = new DatagramSocket(SERVER_PORT)) {

            System.out.println("UDP chat server started on port " + SERVER_PORT);

            // main loop that only receives packets
            while (true) {
                try {
                    byte[] receiveBuffer = new byte[1024];
                    DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);

                    // wait for client packet
                    serverSocket.receive(receivePacket);

                    // submit processing to worker thread
                    executor.submit(() -> handleClientMessage(receivePacket, serverSocket));

                } catch (Exception e) {
                    System.err.println("Error receiving packet: " + e.getMessage());
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            System.err.println("Could not start server: " + e.getMessage());
            e.printStackTrace();
        } finally {
            executor.shutdown();
            System.out.println("Server stopped.");
        }
    }

    /**
     * Processes a single client's packet in a separate thread.
     */
    private static void handleClientMessage(DatagramPacket receivePacket, DatagramSocket serverSocket) {
        try {
            // extract data
            String message = new String(receivePacket.getData(), 0, receivePacket.getLength());
            InetAddress clientAddress = receivePacket.getAddress();
            int clientPort = receivePacket.getPort();

            System.out.printf("Message received: '%s' from %s:%d%n", message, clientAddress, clientPort);

            // if client sends "exit" we just notify it and return
            if ("exit".equalsIgnoreCase(message.trim())) {
                String bye = "Goodbye from server!";
                byte[] sendData = bye.getBytes();
                DatagramPacket sendPacket =
                        new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
                serverSocket.send(sendPacket);
                System.out.println("Client at " + clientAddress + ":" + clientPort + " disconnected.");
                return;
            }

            // build confirmation message
            String confirmation = "Server received: '" + message + "'";
            byte[] sendData = confirmation.getBytes();

            // send response
            DatagramPacket sendPacket =
                    new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
            serverSocket.send(sendPacket);

        } catch (Exception e) {
            System.err.println("Error processing packet: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
