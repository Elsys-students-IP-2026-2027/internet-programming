import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.Scanner;

public class WeatherMulticastServer {

    private static final String MULTICAST_ADDRESS = "230.0.0.1";
    private static final int PORT = 5000;

    public static void main(String[] args) {
        try (MulticastSocket socket = new MulticastSocket();
             Scanner scanner = new Scanner(System.in)) {

            InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
            socket.setTimeToLive(32);

            System.out.println("Weather multicast server started.");
            System.out.println("Group " + MULTICAST_ADDRESS + ":" + PORT);

            while (true) {
                System.out.print("Enter town (or 'exit' to quit): ");
                if (!scanner.hasNextLine()) {
                    break;
                }
                String city = scanner.nextLine().trim();
                if (city.isEmpty()) {
                    continue;
                }
                if ("exit".equalsIgnoreCase(city)) {
                    break;
                }

                String forecast = generateForecast(city);

                byte[] buffer = forecast.getBytes(StandardCharsets.UTF_8);
                DatagramPacket packet =
                        new DatagramPacket(buffer, buffer.length, group, PORT);
                socket.send(packet);

                System.out.println("Broadcasted: " + forecast);
            }

            System.out.println("Server stopped.");
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    private static String generateForecast(String city) {
        Random random = new Random();
        String[] conditions = {"Sunny", "Partly cloudy", "Cloudy", "Windy", "Rainy", "Stormy"};
        String condition = conditions[random.nextInt(conditions.length)];

        int temperature = switch (condition) {
            case "Sunny" -> 20 + random.nextInt(16);        // 20–35°C
            case "Partly cloudy" -> 18 + random.nextInt(11); // 18–28°C
            case "Cloudy", "Windy" -> 10 + random.nextInt(16); // 10–25°C
            case "Rainy" -> 5 + random.nextInt(11);          // 5–15°C
            case "Stormy" -> 5 + random.nextInt(6);          // 5–10°C
            default -> 15 + random.nextInt(10);
        };

        return "Forecast for " + city + ": " + condition + ", " + temperature + "\u00B0C.";
    }
}
