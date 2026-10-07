import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;

public class WeatherMulticastClient {

    private static final String MULTICAST_ADDRESS = "230.0.0.1";
    private static final int PORT = 5000;

    public static void main(String[] args) {

        try (MulticastSocket socket = new MulticastSocket(PORT)) {

            InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
            NetworkInterface netIf = pickInterface();
            InetSocketAddress groupAddress = new InetSocketAddress(group, PORT);

            socket.joinGroup(groupAddress, netIf);
            System.out.println("Joined group " + MULTICAST_ADDRESS + ":" + PORT
                    + " on " + (netIf != null ? netIf.getName() : "default"));
            System.out.println("Waiting for forecasts... (Ctrl+C to stop)");

            byte[] buffer = new byte[1024];
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String message = new String(packet.getData(), 0, packet.getLength(),
                        StandardCharsets.UTF_8);
                System.out.println("Received: " + message);
            }
        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }

    private static NetworkInterface pickInterface() throws SocketException {
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface nif = interfaces.nextElement();
            if (nif.isUp() && nif.supportsMulticast() && !nif.isLoopback()) {
                return nif;
            }
        }
        return null;
    }
}
