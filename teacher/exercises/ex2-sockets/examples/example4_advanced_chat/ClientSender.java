package examples.example4_advanced_chat;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Vector;

class ClientSender implements Runnable {
    private Vector<String> mMessageQueue = new Vector<>();
    private Client mClient;
    private PrintWriter mOut;

    public ClientSender(Client aClient, ServerDispatcher aServerDispatcher) throws IOException {
        mClient = aClient;
        mOut = new PrintWriter(new OutputStreamWriter(mClient.mSocket.getOutputStream()), true);
    }

    public synchronized void sendMessage(String aMessage) {
        mMessageQueue.add(aMessage);
        notify();
    }

    public void sendKeepAlive() {
        sendMessage(AdvancedChatServer.KEEP_ALIVE_MESSAGE);
    }

    private synchronized String getNextMessageFromQueue() throws InterruptedException {
        while (mMessageQueue.isEmpty()) {
            wait();
        }
        String msg = mMessageQueue.get(0);
        mMessageQueue.removeElementAt(0);
        return msg;
    }

    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                String message = getNextMessageFromQueue();
                if (message != null) {
                    mOut.println(message);
                    mOut.flush();
                }
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        } finally {
            cleanup();
        }
    }

    private void cleanup() {
        try {
            mOut.close();
        } catch (Exception e) {
            System.out.println("[Client " + mClient.mClientId + "] Error closing writer: " + e.getMessage());
        }
        if (mClient.mClientListenerThread != null) {
            mClient.mClientListenerThread.interrupt();
        }
    }
}
