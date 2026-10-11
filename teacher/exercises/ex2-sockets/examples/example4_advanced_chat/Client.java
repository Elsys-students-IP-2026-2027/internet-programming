package examples.example4_advanced_chat;

class Client {
    public java.net.Socket mSocket;
    public ClientListener mClientListener;
    public ClientSender mClientSender;
    public Thread mClientListenerThread;
    public Thread mClientSenderThread;
    public int mClientId;
}
