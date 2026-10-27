package examples.example3_tcp_chat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;


public class BenchmarkClient {
    public static void main(String[] args) throws InterruptedException {
        if (args.length != 3) {
            throw new IllegalArgumentException("Parameter(s): <host> <port> <connections>");
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);
        int connections = Integer.parseInt(args[2]);

        AtomicInteger success = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();

        // Release all client threads at the same moment for a fair burst.
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(connections);

        for (int i = 0; i < connections; i++) {
            int id = i;
            Thread.startVirtualThread(() -> {
                try {
                    startGate.await();
                    runOneClient(host, port, id);
                    success.incrementAndGet();
                } catch (Exception e) {
                    failed.incrementAndGet();
                } finally {
                    doneGate.countDown();
                }
            });
        }

        System.out.println("Firing " + connections + " connections at " + host + ":" + port + " ...");
        long start = System.nanoTime();
        startGate.countDown();
        doneGate.await();
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        System.out.println("Done in " + elapsedMs + " ms");
        System.out.println("  success: " + success.get());
        System.out.println("  failed : " + failed.get());
    }

    private static void runOneClient(String host, int port, int id) throws IOException {
        String message = "ping-" + id;

        try (Socket socket = new Socket(host, port);
             OutputStream out = socket.getOutputStream();
             InputStream in = socket.getInputStream()) {

            byte[] payload = message.getBytes(StandardCharsets.UTF_8);
            out.write(payload);
            out.flush();

            byte[] buffer = new byte[payload.length];
            int total = 0;
            while (total < payload.length) {
                int read = in.read(buffer, total, payload.length - total);
                if (read == -1) {
                    throw new IOException("Server closed early for client " + id);
                }
                total += read;
            }
        }
    }
}
