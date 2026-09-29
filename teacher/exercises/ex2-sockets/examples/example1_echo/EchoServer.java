package examples.example1_echo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class EchoServer {
    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 12345;
        System.out.println("Hello World. I am Server!");

        // 1. Създаване на сървърния сокет (ServerSocket).
        // Сървърът се "закача" за локален порт и започва да слуша за TCP заявки.
        try (ServerSocket server = new ServerSocket(port)) {
            while (true) {
                // 2. Приемане на клиентска връзка.
                // accept() е блокиращ метод: изчаква клиент да завърши TCP handshake.
                System.out.println("Waiting for client...");
                Socket socket = server.accept();
                System.out.println("Connection established: " + socket.getInetAddress());

                // 3. Извличане на мрежовите потоци от клиентския сокет.
                // InputStream е за четене на данни от клиента,
                // а OutputStream е за изпращане на отговор обратно.
                try (BufferedReader in = new BufferedReader(
                         new InputStreamReader(socket.getInputStream()));
                      PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

                    // 4. Ехо-цикъл за обработка на данните.
                    // readLine() връща null, когато клиентът затвори своята страна на връзката.
                    String line;
                    while ((line = in.readLine()) != null) {
                        System.out.println("Received: " + line);
                        // Връщаме прочетения ред обратно към клиента.
                        out.println(line);
                    }
                } catch (IOException e) {
                    System.err.println("Client error: " + e.getMessage());
                }

                // 5. Затваряне на клиентската връзка.
                // try-with-resources затваря socket автоматично и освобождава ресурсите.
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }
}
