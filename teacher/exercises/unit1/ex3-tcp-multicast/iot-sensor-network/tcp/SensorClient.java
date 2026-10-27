import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Random;

/**
 * IoT sensor network — TCP client
 * Simulates one IoT device with a unique DEVICE_ID. Every 5 seconds it sends
 * a random temperature and humidity to the server in the format
 * DEVICE_ID:TEMP:HUMIDITY and prints the returned average values.
 */
public class SensorClient {

    private static final String HOST = "localhost";
    private static final int PORT = 5555;
    private static final int SEND_INTERVAL_MS = 5_000;

    public static void main(String[] args) {
        String deviceId = args.length > 0 ? args[0] : "sensor1";
        Random random = new Random();

        System.out.println("Sensor " + deviceId + " connecting to " + HOST + ":" + PORT + "...");

        try (Socket socket = new Socket(HOST, PORT);
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(new OutputStreamWriter(
                     socket.getOutputStream(), StandardCharsets.UTF_8), true)) {

            System.out.println("Connected. Sending data every 5 seconds...");

            while (true) {
                // Random temperature - not to exceed 30 degrees
                // Random humidity - not to exceed 100%
                double temp = 18 + random.nextDouble() * 12;
                double humidity = 40 + random.nextDouble() * 40;

                String message = String.format("%s:%.1f:%.1f", deviceId, temp, humidity);
                out.println(message);
                System.out.println("TX: " + message);

                // Read the server's response (the average values).
                String response = in.readLine();
                if (response == null) {
                    System.out.println("Server closed the connection.");
                    break;
                }
                System.out.println("RX: " + response);

                Thread.sleep(SEND_INTERVAL_MS);
            }
        } catch (IOException e) {
            System.err.println("Connection error: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
