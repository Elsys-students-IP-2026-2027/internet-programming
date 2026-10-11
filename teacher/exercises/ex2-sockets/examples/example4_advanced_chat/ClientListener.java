package examples.example4_advanced_chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;

class ClientListener implements Runnable {
    private ServerDispatcher mServerDispatcher;
    private Client mClient;
    private BufferedReader mSocketReader;

    public ClientListener(Client aClient, ServerDispatcher aSrvDispatcher) throws IOException {
        mClient = aClient;
        mServerDispatcher = aSrvDispatcher;
        mClient.mSocket.setSoTimeout(AdvancedChatServer.CLIENT_READ_TIMEOUT);
        mSocketReader = new BufferedReader(new InputStreamReader(mClient.mSocket.getInputStream()));
    }

    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    String message = mSocketReader.readLine();
                    if (message == null) {
                        break;
                    }
                    if (!message.trim().isEmpty()) {
                        mServerDispatcher.dispatchMessage(mClient, message);
                    }
                } catch (SocketTimeoutException ste) {
                    mClient.mClientSender.sendKeepAlive();
                }
            }
        } catch (IOException ioex) {
            if (!Thread.currentThread().isInterrupted()) {
                System.out.println("[Client " + mClient.mClientId + "] Read error: " + ioex.getMessage());
            }
        } finally {
            cleanup();
        }
    }

    private void cleanup() {
        try {
            mSocketReader.close();
        } catch (IOException e) {
            System.out.println("[Client " + mClient.mClientId + "] Error closing reader: " + e.getMessage());
        }
        if (mClient.mClientSenderThread != null) {
            mClient.mClientSenderThread.interrupt();
        }
        mServerDispatcher.deleteClient(mClient);
    }
}
