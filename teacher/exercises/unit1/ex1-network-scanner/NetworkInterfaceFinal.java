import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class NetworkInterfaceFinal {

    public static void main(String[] args) {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces == null) {
                System.out.println("No network interfaces were found.");
                return;
            }

            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                printInterface(networkInterface);
                System.out.println("---------------------------------------------------");
            }
        } catch (SocketException e) {
            System.out.println("Error while reading network interfaces: " + e.getMessage());
        }
    }

    private static void printInterface(NetworkInterface networkInterface) throws SocketException {
        System.out.println("Interface: " + networkInterface.getName());
        System.out.println("Description: " + networkInterface.getDisplayName());
        System.out.println("Index: " + networkInterface.getIndex());
        System.out.println("MTU: " + networkInterface.getMTU());
        System.out.println("Virtual: " + networkInterface.isVirtual());
        System.out.println("Point-to-point: " + networkInterface.isPointToPoint());
        System.out.println("Interface is active: " + networkInterface.isUp());
        System.out.println("Support multicast: " + networkInterface.supportsMulticast());
        System.out.println("Loopback interface: " + networkInterface.isLoopback());

        byte[] mac = networkInterface.getHardwareAddress();
        System.out.println("MAC address: " + (mac != null ? formatMac(mac) : "No information."));

        Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
        while (inetAddresses.hasMoreElements()) {
            InetAddress inetAddress = inetAddresses.nextElement();
            System.out.println("IP address: " + inetAddress.getHostAddress());
        }

        for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
            InetAddress address = interfaceAddress.getAddress();
            if (address != null) {
                System.out.println("Interface address: " + address.getHostAddress()
                        + " / " + interfaceAddress.getNetworkPrefixLength());
            }
            InetAddress broadcast = interfaceAddress.getBroadcast();
            if (broadcast != null) {
                System.out.println("Broadcast address: " + broadcast.getHostAddress());
            }
        }
    }

    private static String formatMac(byte[] mac) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < mac.length; i++) {
            if (i > 0) {
                builder.append('-');
            }
            builder.append(String.format("%02X", mac[i]));
        }
        return builder.toString();
    }
}
