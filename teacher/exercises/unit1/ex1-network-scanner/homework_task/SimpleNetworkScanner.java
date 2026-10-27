package homework_task;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;


public class SimpleNetworkScanner {

  private static final int TIMEOUT_MS = 600;
  private static final String RESULTS_FILE = "results.txt";

  private static final Map<String, FileExportData> results = new LinkedHashMap<>();

  public static void main(String[] args) {
    try {
      System.out.println("=== Simple Network Scanner ===\n");

      String baseIP = getBaseIP();

      System.out.println("\n=== Scanning Last Octet (" + baseIP + "1-254) ===\n");
      scanLastOctet(baseIP);

      System.out.println("\n=== Reading ARP Table for MAC addresses ===\n");
      Map<String, String> arpMap = buildArpMap();

      enrichResults(arpMap);

      System.out.println("\n=== Scan Summary ===");
      System.out.println("Total hosts scanned: " + results.size());

      System.out.println("\n=== Writing Results to File ===\n");
      writeResultsToFile();

      System.out.println("Scan complete. Results saved to " + RESULTS_FILE);
    } catch (Exception e) {
      System.err.println("Fatal error: " + e.getMessage());
    }
  }

  private static String getBaseIP() {
    try {
      Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
      while (interfaces.hasMoreElements()) {
        NetworkInterface ni = interfaces.nextElement();
        if (ni.isUp() && !ni.isLoopback() && !ni.isVirtual()) {
          Enumeration<InetAddress> addresses = ni.getInetAddresses();
          while (addresses.hasMoreElements()) {
            InetAddress addr = addresses.nextElement();
            String ip = addr.getHostAddress();
            if (ip.contains(".") && !ip.startsWith("127.")) {
              return extractBaseIP(ip);
            }
          }
        }
      }
    } catch (SocketException e) {
      System.err.println("Error determining base IP: " + e.getMessage());
    }
    return "192.168.1.";
  }

  private static void scanLastOctet(String baseIP) {
    for (int i = 1; i <= 254; i++) {
      String ip = baseIP + i;

      System.out.print("\rScanning " + ip + " ... (" + i + "/254)   ");

      try {
        InetAddress host = InetAddress.getByName(ip);
        boolean reachable = host.isReachable(TIMEOUT_MS);

        results.put(ip, new FileExportData(ip, reachable, "N/A", "-"));

        if (reachable) {
          System.out.println("\r" + ip + " is reachable          ");
        }
      } catch (IOException e) {
        results.put(ip, new FileExportData(ip, false, "N/A", "-"));
      }
    }
    System.out.println("\rScan finished.                    ");
  }

  private static Map<String, String> buildArpMap() {
    Map<String, String> arpMap = new LinkedHashMap<>();
    String osName = System.getProperty("os.name").toLowerCase();
    ProcessBuilder pb = osName.contains("win")
        ? new ProcessBuilder("cmd", "/c", "arp -a")
        : new ProcessBuilder("arp", "-a");

    try {
      Process process = pb.start();
      try (BufferedReader reader = new BufferedReader(
          new InputStreamReader(process.getInputStream()))) {
        String line;
        while ((line = reader.readLine()) != null) {
          parseArpLine(line, arpMap);
        }
      }
      process.waitFor();
    } catch (IOException e) {
      System.err.println("Error reading ARP table: " + e.getMessage());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    return arpMap;
  }

  private static void parseArpLine(String line, Map<String, String> arpMap) {

    line = line.trim();
    if (line.isEmpty() || line.toLowerCase().contains("address") || line.contains("Interface")) {
      return;
    }

    String ip = null;
    String mac = null;

    String[] parts = line.split("\\s+");
    for (String part : parts) {
      if (part.contains(".") && !part.contains("(") && isValidIP(part)) {
        ip = part;
      }
      if ((part.contains(":") || part.contains("-")) && isMacAddress(part)) {
        mac = normalizeMac(part);
      }
    }

    int parenStart = line.indexOf("(");
    int parenEnd = line.indexOf(")");
    if (parenStart != -1 && parenEnd != -1) {
      String inParen = line.substring(parenStart + 1, parenEnd);
      if (isValidIP(inParen)) {
        ip = inParen;
      }
    }

    if (ip != null && mac != null) {
      arpMap.put(ip, mac);
    }
  }

  private static void enrichResults(Map<String, String> arpMap) {
    for (Map.Entry<String, FileExportData> entry : results.entrySet()) {
      FileExportData result = entry.getValue();

      String mac = arpMap.get(result.ip());
      String macAddress = (mac != null) ? mac : result.mac();

      String hostname = result.hostname();
      if (result.reachable()) {
        hostname = performReverseDNS(result.ip());
      }

      entry.setValue(new FileExportData(
          result.ip(), result.reachable(), hostname, macAddress));
    }
  }

  private static boolean isValidIP(String str) {
    String[] parts = str.split("\\.");
    if (parts.length != 4) {
      return false;
    }
    try {
      for (String part : parts) {
        int num = Integer.parseInt(part);
        if (num < 0 || num > 255) {
          return false;
        }
      }
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  private static boolean isMacAddress(String str) {
    String pattern = "([0-9A-Fa-f]{1,2}[:-]){5}([0-9A-Fa-f]{1,2})";
    return str.matches(pattern);
  }

  private static String normalizeMac(String mac) {
    return mac.replace("-", ":").toUpperCase();
  }

  private static String performReverseDNS(String ip) {
    try {
      InetAddress address = InetAddress.getByName(ip);
      String host = address.getCanonicalHostName();
      if (host.equals(address.getHostAddress())) {
        return "N/A";
      }
      return host;
    } catch (IOException e) {
      return "N/A";
    }
  }

  private static void writeResultsToFile() {
    try (FileWriter fw = new FileWriter(RESULTS_FILE);
         BufferedWriter bw = new BufferedWriter(fw)) {

      bw.write("=== Network Scan Results ===\n");
      bw.write("Scan Date: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n");
      bw.write("\n");
      bw.write(String.format("%-15s | %-9s | %-20s | %s%n",
          "IP", "Reachable", "Reverse DNS", "MAC"));
      bw.write("----------------+-----------+----------------------+--------------------\n");

      for (FileExportData result : results.values()) {
        if (result.reachable() || (result.mac() != null && !result.mac().equals("-"))) {
          bw.write(result.toString() + "\n");
        }
      }

      System.out.println("Results written to " + RESULTS_FILE);
    } catch (IOException e) {
      System.err.println("Error writing results to file: " + e.getMessage());
    }
  }

  private static String extractBaseIP(String ip) {
    String[] parts = ip.split("\\.");
    if (parts.length == 4) {
      return parts[0] + "." + parts[1] + "." + parts[2] + ".";
    }
    return "0.0.0.";
  }
}