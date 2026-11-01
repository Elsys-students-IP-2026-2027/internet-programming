package multicast.src;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Random;

public class WeatherMulticastServer {
    private static final String MULTICAST_ADDRESS = "224.0.0.1";
    private static final int PORT = 5000;

    public static void main(String[] args) {
        try (MulticastSocket multicastSocket = new MulticastSocket()) {
            InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
            System.out.println("Server is started:");

            while (true) {
                String city = System.console().readLine("Enter town (или 'exit' за изход): ");
                if ("exit".equalsIgnoreCase(city.trim())) {
                    break;
                }

                String weatherForecast = generateWeatherForecast(city);
                byte[] buffer = weatherForecast.getBytes();

                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, PORT);
                multicastSocket.send(packet);
                System.out.println("Weather prediction for " + city + ": " + weatherForecast);
            }
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static String generateWeatherForecast(String city) {
        Random random = new Random();

        String[] conditions = {"Sunny", "Partly cloudy", "Cloudy", "Windy", "Rainy", "Stormy"};
        String condition = conditions[random.nextInt(conditions.length)];

        int temperature;

        switch (condition) {
            case "Sunny":
                temperature = 20 + random.nextInt(16); // 20–35°C
                break;
            case "Partly cloudy":
                temperature = 18 + random.nextInt(11); // 18–28°C
                break;
            case "Cloudy":
            case "Windy":
                temperature = 10 + random.nextInt(16); // 10–25°C
                break;
            case "Rainy":
                temperature = 5 + random.nextInt(11); // 5–15°C
                break;
            case "Stormy":
                temperature = 5 + random.nextInt(6); // 5–10°C
                break;
            default:
                temperature = 15 + random.nextInt(10);
                break;
        }

        return "Forecast for " + city + ": " + condition + ", " + temperature + "°C.";
    }
}
