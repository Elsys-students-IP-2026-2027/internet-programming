

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientFinal {
    public static void main(String[] args) {
        System.out.println("Hello World. I am Client!");
        try (Socket socket = new Socket("localhost", 9708);
             BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            System.out.println("Connected to server. Type messages (type 'exit' to quit):");

            String message;
            while (true) {
                System.out.print("You: ");
                message = userInput.readLine();
                if (message == null) break;

                out.println(message);
                String response = in.readLine();
                if (response != null) {
                    System.out.println(response);
                }

                if (message.equalsIgnoreCase("exit")) {
                    System.out.println("Exiting client...");
                    break;
                }
            }

        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
