package examples.example4_advanced_chat;

import java.io.IOException;
import java.util.Vector;

class ServerDispatcher extends Thread {
    private Vector<String> mMessageQueue = new Vector<>();
    private Vector<Client> mClients = new Vector<>();
    private volatile boolean mRunning = true;

    public synchronized void addClient(Client aClient) {
        mClients.add(aClient);
        System.out.println("[Server] Total clients connected: " + mClients.size());
    }

    public synchronized void deleteClient(Client aClient) {
        int index = mClients.indexOf(aClient);
        if (index != -1) {
            mClients.removeElementAt(index);
            System.out.println("[Client " + aClient.mClientId + "] Disconnected. Total clients: " + mClients.size());
            try {
                aClient.mSocket.close();
            } catch (IOException e) {
                System.out.println("[Client " + aClient.mClientId + "] Error closing socket: " + e.getMessage());
            }
        }
    }

    public synchronized void dispatchMessage(Client aClient, String aMessage) {
        String senderInfo = "[Client " + aClient.mClientId + "] ";
        String fullMessage = senderInfo + aMessage;
        mMessageQueue.add(fullMessage);
        notify();
    }

    private synchronized String getNextMessageFromQueue() throws InterruptedException {
        while (mMessageQueue.isEmpty() && mRunning) {
            wait();
        }
        if (mMessageQueue.isEmpty()) {
            return null;
        }
        String msg = mMessageQueue.get(0);
        mMessageQueue.removeElementAt(0);
        return msg;
    }

    private synchronized void sendMessageToAllClients(String aMessage) {
        for (Client client : mClients) {
            client.mClientSender.sendMessage(aMessage);
        }
    }

    public void shutdown() {
        mRunning = false;
        synchronized (this) {
            notifyAll();
        }
    }

    public void run() {
        try {
            while (mRunning) {
                String message = getNextMessageFromQueue();
                if (message != null) {
                    sendMessageToAllClients(message);
                }
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
