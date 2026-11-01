package multicast.src;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketAddress;

public class WeatherMulticastClient {
    private static final String MULTICAST_ADDRESS = "224.0.0.1";
    private static final int PORT = 5000;

    public static void main(String[] args) {
        try (MulticastSocket multicastSocket = new MulticastSocket(PORT)) {

            NetworkInterface networkInterface = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
            if (networkInterface == null) {
                networkInterface = NetworkInterface.getByName("eth0");
            }
            InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
            SocketAddress groupAddress = new InetSocketAddress(group, PORT);
            multicastSocket.joinGroup(groupAddress, networkInterface);
            System.out.println("Client join the group.Wait to receive forecast...");

            while (true) {
                byte[] buffer = new byte[256];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                multicastSocket.receive(packet);

                String message = new String(packet.getData(), 0, packet.getLength());
                System.out.println("Received message: " + message);
            }
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}