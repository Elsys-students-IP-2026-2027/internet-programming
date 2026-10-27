import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;


public class SensorClient {

  private static final String DEFAULT_HOST = "localhost";
  private static final int PORT = 5555;
  private static final int SEND_INTERVAL_MS = 5_000;
  private static final int RECEIVE_TIMEOUT_MS = 5_000;
  private static final int MAX_PACKET_SIZE = 1_024;

  public static void main(String[] args) {
    String host = args.length > 0 ? args[0] : DEFAULT_HOST;
    String deviceId = args.length > 1 ? args[1] : "sensor-" + UUID.randomUUID();
    Random random = new Random();

    try (DatagramSocket socket = new DatagramSocket()) {
      socket.setSoTimeout(RECEIVE_TIMEOUT_MS);
      InetAddress address = InetAddress.getByName(host);
      System.out.printf("Sensor %s sending UDP data to %s:%d%n", deviceId, host, PORT);

      while (true) {
        double temperature = 18 + random.nextDouble() * 12;
        double humidity = 40 + random.nextDouble() * 40;
        String message = String.format(
            Locale.ROOT, "%s:%.1f:%.1f", deviceId, temperature, humidity);
        byte[] requestBytes = message.getBytes(StandardCharsets.UTF_8);
        socket.send(new DatagramPacket(requestBytes, requestBytes.length, address, PORT));
        System.out.println("TX: " + message);

        byte[] responseBuffer = new byte[MAX_PACKET_SIZE];
        DatagramPacket response = new DatagramPacket(responseBuffer, responseBuffer.length);
        try {
          socket.receive(response);
          String responseText = new String(
              response.getData(),
              response.getOffset(),
              response.getLength(),
              StandardCharsets.UTF_8);
          System.out.println("RX: " + responseText);
          if (responseText.equals("ERROR:device id already in use")) {
            break;
          }
        } catch (SocketTimeoutException e) {
          System.out.println("No server response within 5 seconds.");
        }

        Thread.sleep(SEND_INTERVAL_MS);
      }
    } catch (IOException e) {
      System.err.println("UDP client error: " + e.getMessage());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
