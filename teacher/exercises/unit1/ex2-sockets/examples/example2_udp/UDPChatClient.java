package examples.example2_udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Scanner;

public class UDPChatClient {

    private static final int SERVER_PORT = 9876;
    private static final String SERVER_ADDRESS = "localhost";

    public static void main(String[] args) throws IOException {
        // 1. Create an unbound UDP socket for sending and receiving datagrams.
        DatagramSocket clientSocket = new DatagramSocket();
        InetAddress serverAddress = InetAddress.getByName(SERVER_ADDRESS);

        Scanner scanner = new Scanner(System.in);

        System.out.println("UDP Chat Client");
        System.out.println("Type 'exit' to quit");

        while (true) {
            // 2. Read a message from the console.
            System.out.print("Enter message: ");
            String message = scanner.nextLine();

            if ("exit".equalsIgnoreCase(message)) {
                // Send the exit command to the server before leaving.
                byte[] sendData = message.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, SERVER_PORT);
                clientSocket.send(sendPacket);
                break;
            }

            // 3. Put the text into a UDP datagram and send it to the server.
            byte[] sendData = message.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, SERVER_PORT);
            clientSocket.send(sendPacket);

            // 4. Wait for the server's response datagram.
            byte[] receiveData = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            clientSocket.receive(receivePacket);

            // 5. Convert the received bytes back to a string and print them.
            String response = new String(receivePacket.getData(), 0, receivePacket.getLength());
            System.out.println("Server: " + response);
        }

        // 6. Close resources.
        clientSocket.close();
        scanner.close();
    }
}
