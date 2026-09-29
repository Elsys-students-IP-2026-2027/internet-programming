package examples.example2_udp;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPChatServer {

    private static final int SERVER_PORT = 9876;

    public static void main(String[] args) throws Exception {
        // 1. Create a UDP socket bound to a fixed local port.
        // Unlike TCP, UDP does not use accept() or separate per-client sockets.
        DatagramSocket serverSocket = new DatagramSocket(SERVER_PORT);
        byte[] receiveData = new byte[1024];

        System.out.println("UDP chat server started on port " + SERVER_PORT);

        while (true) {
            // 2. Prepare a datagram packet that will hold the incoming message.
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

            // 3. Receive one UDP datagram from any client.
            serverSocket.receive(receivePacket);

            // 4. Extract the text and the sender's address/port from the packet.
            String message = new String(receivePacket.getData(), 0, receivePacket.getLength());
            InetAddress clientAddress = receivePacket.getAddress();
            int clientPort = receivePacket.getPort();

            System.out.println("Message is received: " + message + " from " + clientAddress + ":" + clientPort);
            if ("exit".equalsIgnoreCase(message)) {
                System.out.println("Server is stopped");
                break;
            }

            // 5. Build a response packet and send it back to the original sender.
            String confirmation = "Server receive message '" + message + "'";
            byte[] sendData = confirmation.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
            serverSocket.send(sendPacket);
        }
        serverSocket.close();
        System.out.println("Server socket closed.");
    }
}
