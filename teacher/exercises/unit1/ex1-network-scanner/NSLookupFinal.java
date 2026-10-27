import java.io.IOException;
import java.net.InetAddress;

public class NSLookupFinal {

    private static final String DEFAULT_DOMAIN = "www.yahoo.com";
    private static final String NSLOOKUP_DOMAIN = "google.com";
    private static final String REVERSE_DNS_IP = "8.8.8.8";
    private static final String[] PING_TARGETS = {"127.0.0.1", REVERSE_DNS_IP};

    static void main(String[] args) {
        try {
            printHostDetails("Local host", InetAddress.getLocalHost());
            printHostDetails("Loopback", InetAddress.getLoopbackAddress());
            printHostDetails("Reverse DNS lookup", InetAddress.getByName(REVERSE_DNS_IP));
            printHostDetails("Domain lookup for " + DEFAULT_DOMAIN, InetAddress.getByName(DEFAULT_DOMAIN));

            printLookupResults(NSLOOKUP_DOMAIN);
            printLookupResults(args.length > 0 ? args[0] : DEFAULT_DOMAIN);
            printPingResults(args.length > 1 ? new String[]{args[1]} : PING_TARGETS);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void printLookupResults(String host) throws IOException {
        System.out.println("NSLookup for " + host + ":");
        for (InetAddress address : InetAddress.getAllByName(host)) {
            printHostDetails("Resolved address", address);
        }
    }

    private static void printPingResults(String[] targets) throws IOException {
        for (String target : targets) {
            InetAddress ping = InetAddress.getByName(target);
            System.out.println("Sending ping request to " + target);
            if (ping.isReachable(5000)) {
                System.out.println("Host is reachable");
            } else {
                System.out.println("Sorry! We can't reach this host");
            }
        }
    }

    private static void printHostDetails(String label, InetAddress address) {
        System.out.println(label + ":");
        System.out.println("  Host name: " + address.getHostName());
        System.out.println("  Canonical host name: " + address.getCanonicalHostName());
        System.out.println("  IP address: " + address.getHostAddress());
    }
}
