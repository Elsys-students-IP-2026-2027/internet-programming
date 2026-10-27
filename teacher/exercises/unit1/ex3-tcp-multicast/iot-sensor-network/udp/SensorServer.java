import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;


 // A sensor sends DEVICE_ID:TEMP:HUMIDITY. The server stores the latest valid
//reading for each active sensor and replies with AVG:<temp>:<humidity>(devices=<count>).

public class SensorServer {

    private static final int PORT = 5555;
    private static final int ACTIVE_TIMEOUT_MS = 30_000;
    private static final int RECEIVE_TIMEOUT_MS = 1_000;
    private static final int MAX_PACKET_SIZE = 1_024;

    private record Reading(double temperature, double humidity, long lastSeen) {}

    // DatagramSocket receives and updates readings on this single event loop.
    private static final Map<String, Reading> readings = new HashMap<>();

    public static void main(String[] args) {
        System.out.println("UDP sensor server starting on port " + PORT + "...");

        try (DatagramSocket socket = new DatagramSocket(PORT)) {
            socket.setSoTimeout(RECEIVE_TIMEOUT_MS);
            byte[] buffer = new byte[MAX_PACKET_SIZE];

            while (true) {
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                try {
                    socket.receive(request);
                    String message = new String(
                            request.getData(),
                            request.getOffset(),
                            request.getLength(),
                            StandardCharsets.UTF_8);
                    removeInactiveSensors();
                    String response = process(message);
                    byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
                    DatagramPacket reply = new DatagramPacket(
                            responseBytes,
                            responseBytes.length,
                            request.getAddress(),
                            request.getPort());
                    socket.send(reply);
                    System.out.println("RX: " + message + " -> TX: " + response);
                } catch (SocketTimeoutException e) {
                    // The loop also periodically expires sensors that stopped sending.
                }
                removeInactiveSensors();
            }
        } catch (IOException e) {
            System.err.println("UDP server error: " + e.getMessage());
        }
    }

    private static String process(String message) {
        String[] parts = message.split(":", -1);
        if (parts.length != 3) {
            return "ERROR:invalid data";
        }

        String deviceId = parts[0].trim();
        if (deviceId.isEmpty()) {
            return "ERROR:invalid data";
        }

        try {
            double temperature = Double.parseDouble(parts[1].trim());
            double humidity = Double.parseDouble(parts[2].trim());
            if (!Double.isFinite(temperature)
                    || !Double.isFinite(humidity)
                    || temperature < -40
                    || temperature > 50) {
                return "ERROR:invalid data";
            }

            readings.put(deviceId, new Reading(temperature, humidity, System.nanoTime()));
            return averages();
        } catch (NumberFormatException e) {
            return "ERROR:invalid data";
        }
    }

    private static void removeInactiveSensors() {
        long cutoff = System.nanoTime() - ACTIVE_TIMEOUT_MS * 1_000_000L;
        Iterator<Reading> iterator = readings.values().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().lastSeen() < cutoff) {
                iterator.remove();
            }
        }
    }

    private static String averages() {
        double temperatureTotal = 0;
        double humidityTotal = 0;
        for (Reading reading : readings.values()) {
            temperatureTotal += reading.temperature();
            humidityTotal += reading.humidity();
        }

        int deviceCount = readings.size();
        double averageTemperature = deviceCount == 0 ? 0 : temperatureTotal / deviceCount;
        double averageHumidity = deviceCount == 0 ? 0 : humidityTotal / deviceCount;
        return String.format(
                Locale.ROOT,
                "AVG:%.1f:%.1f (devices=%d)",
                averageTemperature,
                averageHumidity,
                deviceCount);
    }
}
