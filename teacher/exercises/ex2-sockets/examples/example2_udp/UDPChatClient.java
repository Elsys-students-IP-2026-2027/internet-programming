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
        DatagramSocket clientSocket = new DatagramSocket();
        InetAddress serverAddress = InetAddress.getByName(SERVER_ADDRESS);

        Scanner scanner = new Scanner(System.in);

        System.out.println("UDP Chat Client");
        System.out.println("Type 'exit' to quit");

        while (true) {
            System.out.print("Enter message: ");
            String message = scanner.nextLine();

            if ("exit".equalsIgnoreCase(message)) {
                byte[] sendData = message.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, SERVER_PORT);
                clientSocket.send(sendPacket);
                break;
            }

            byte[] sendData = message.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, SERVER_PORT);
            clientSocket.send(sendPacket);

            byte[] receiveData = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            clientSocket.receive(receivePacket);

            String response = new String(receivePacket.getData(), 0, receivePacket.getLength());
            System.out.println("Server: " + response);
        }

        clientSocket.close();
        scanner.close();
    }
}
