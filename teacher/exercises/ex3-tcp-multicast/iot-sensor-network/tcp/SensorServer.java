import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class SensorServer {

  private static final int PORT = 5555;
  private static final int CLIENT_TIMEOUT_MS = 30_000; // timeout

  private record Reading(double temperature, double humidity, long lastSeen) {
  }

  // Key = DEVICE_ID; values expire after the device's inactivity timeout.
  private static final Map<String, Reading> readings = new ConcurrentHashMap<>();

  public static void main(String[] args) {
    System.out.println("Sensor server starting on port " + PORT + "...");

    try (ServerSocket serverSocket = new ServerSocket(PORT)) {
      System.out.println("Server is listening. Waiting for sensors...");

      while (true) {
        Socket socket = serverSocket.accept();
        System.out.println("Sensor connected: " + socket.getInetAddress().getHostAddress());

        Thread.ofVirtual().start(() -> handleClient(socket));
      }
    } catch (IOException e) {
      System.err.println("Server error: " + e.getMessage());
    }
  }

  private static void handleClient(Socket socket) {
    try (Socket client = socket;
         BufferedReader in = new BufferedReader(
             new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
         PrintWriter out = new PrintWriter(new java.io.OutputStreamWriter(
             client.getOutputStream(), StandardCharsets.UTF_8), true)) {

      client.setSoTimeout(CLIENT_TIMEOUT_MS);

      String line;
      while ((line = in.readLine()) != null) {
        String response = process(line);
        out.println(response);
        System.out.println("RX: " + line + "  ->  TX: " + response);
      }
    } catch (SocketTimeoutException e) {
      System.out.println("Sensor timed out (inactive) — closing connection.");
      removeInactiveReadings();
    } catch (IOException e) {
      System.err.println("Client error: " + e.getMessage());
    } finally {
      System.out.println("Sensor disconnected.");
    }
  }

  private static String process(String line) {
    String[] parts = line.split(":", -1);
    if (parts.length != 3) {
      return "ERROR:invalid data";
    }
    try {
      String deviceId = parts[0].trim();
      double temp = Double.parseDouble(parts[1].trim());
      double humidity = Double.parseDouble(parts[2].trim());
      if (deviceId.isEmpty()
          || !Double.isFinite(temp)
          || !Double.isFinite(humidity)
          || temp < -40
          || temp > 50) {
        return "ERROR:invalid data";
      }

      removeInactiveReadings();
      readings.put(deviceId, new Reading(temp, humidity, System.nanoTime()));
      return averages();
    } catch (NumberFormatException e) {
      return "ERROR:invalid data";
    }
  }

  private static String averages() {
    removeInactiveReadings();
    double sumTemp = 0;
    double sumHum = 0;
    for (Reading reading : readings.values()) {
      sumTemp += reading.temperature();
      sumHum += reading.humidity();
    }
    int n = readings.size();
    double avgTemp = n == 0 ? 0 : sumTemp / n;
    double avgHum = n == 0 ? 0 : sumHum / n;
    return String.format(Locale.ROOT, "AVG:%.1f:%.1f (devices=%d)", avgTemp, avgHum, n);
  }

  private static void removeInactiveReadings() {
    long cutoff = System.nanoTime() - CLIENT_TIMEOUT_MS * 1_000_000L;
    readings.forEach((deviceId, reading) -> {
      if (reading.lastSeen() < cutoff) {
        readings.remove(deviceId, reading);
      }
    });
  }
}
