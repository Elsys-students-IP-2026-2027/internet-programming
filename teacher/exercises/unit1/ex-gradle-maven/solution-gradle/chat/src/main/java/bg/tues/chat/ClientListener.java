package bg.tues.chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

final class ClientListener implements Runnable {
    private final Client client;
    private final ServerDispatcher dispatcher;
    private final BufferedReader socketReader;

    ClientListener(Client client, ServerDispatcher dispatcher) throws IOException {
        this.client = client;
        this.dispatcher = dispatcher;
        this.socketReader = new BufferedReader(new InputStreamReader(client.socket.getInputStream(), StandardCharsets.UTF_8));
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted() && !client.disconnected.get()) {
                String message = socketReader.readLine();
                if (message == null) {
                    break;
                }
                String trimmed = message.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                if ("exit".equalsIgnoreCase(trimmed) || "/quit".equalsIgnoreCase(trimmed)) {
                    dispatcher.requestDisconnect(client, true);
                    continue;
                }
                if (!dispatcher.dispatchMessage(client, message)) {
                    dispatcher.requestDisconnect(client, true);
                    break;
                }
            }
        } catch (IOException ignored) {
        } finally {
            dispatcher.requestDisconnect(client, true);
            try {
                socketReader.close();
            } catch (IOException ignored) {
            }
        }
    }
}
