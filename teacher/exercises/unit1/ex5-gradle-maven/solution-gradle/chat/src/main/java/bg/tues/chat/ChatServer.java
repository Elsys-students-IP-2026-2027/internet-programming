package bg.tues.chat;

import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class ChatServer implements AutoCloseable {
  private static final int DEFAULT_PORT = 2002;
  private static final int DEFAULT_DISPATCH_QUEUE_CAPACITY = 1_024;
  private static final int DEFAULT_CLIENT_QUEUE_CAPACITY = 128;
  private static final Duration DEFAULT_WRITE_TIMEOUT = Duration.ofSeconds(2);
  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

  private final int listeningPort;
  private final int dispatchQueueCapacity;
  private final int clientQueueCapacity;
  private final Duration writeTimeout;
  private final PrintStream log;
  private final ScheduledExecutorService writeDeadlineScheduler;
  private final Object registryLock = new Object();
  private final AtomicInteger nextClientNumber = new AtomicInteger(1);
  private final AtomicBoolean started = new AtomicBoolean(false);
  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final Map<String, Client> nicknames = new HashMap<>();
  private final java.util.Set<Client> clients = ConcurrentHashMap.newKeySet();

  private volatile ServerSocket serverSocket;
  private volatile ServerDispatcher dispatcher;
  private volatile Thread acceptThread;

  public ChatServer(int port) {
    this(port, DEFAULT_DISPATCH_QUEUE_CAPACITY, DEFAULT_CLIENT_QUEUE_CAPACITY, DEFAULT_WRITE_TIMEOUT, System.out);
  }

  ChatServer(int port, int dispatchQueueCapacity, int clientQueueCapacity, Duration writeTimeout, PrintStream log) {
    this.listeningPort = port <= 0 ? 0 : port;
    this.dispatchQueueCapacity = dispatchQueueCapacity;
    this.clientQueueCapacity = clientQueueCapacity;
    this.writeTimeout = writeTimeout;
    this.log = log;
    this.writeDeadlineScheduler = Executors.newScheduledThreadPool(1);
  }

  public static void main(String[] args) throws IOException {
    int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
    ChatServer server = new ChatServer(port).start();
    Runtime.getRuntime().addShutdownHook(Thread.ofPlatform().unstarted(() -> {
      try {
        server.close();
      } catch (IOException ignored) {
      }
    }));
    try {
      Thread.currentThread().join();
    } catch (InterruptedException ignored) {
      Thread.currentThread().interrupt();
    }
  }

  public ChatServer start() throws IOException {
    if (closed.get()) {
      throw new IllegalStateException("Server is already closed.");
    }
    if (!started.compareAndSet(false, true)) {
      return this;
    }
    try {
      serverSocket = new ServerSocket(listeningPort);
      dispatcher = new ServerDispatcher(this, dispatchQueueCapacity);
      dispatcher.start();
      acceptThread = Thread.startVirtualThread(this::acceptLoop);
      log.println("ChatServer started on port " + getPort());
      return this;
    } catch (IOException | RuntimeException e) {
      cleanupFailedStart();
      started.set(false);
      throw e;
    }
  }

  public int getPort() {
    ServerSocket socket = serverSocket;
    return socket == null ? -1 : socket.getLocalPort();
  }

  void handleClientMessage(Client client, String rawMessage) {
    String trimmed = rawMessage.trim();
    if (trimmed.equalsIgnoreCase("/who")) {
      sendWho(client);
      return;
    }
    if (trimmed.toLowerCase().startsWith("/nick")) {
      renameClient(client, trimmed);
      return;
    }
    broadcastToOthers(client, formatChatLine(client.nickname, rawMessage));
  }

  void disconnect(Client client, boolean announceDeparture) {
    if (client == null || !client.disconnected.compareAndSet(false, true)) {
      return;
    }
    closeClientSocket(client);
    clients.remove(client);
    releaseNickname(client);
    if (announceDeparture) {
      broadcastToOthers(client, formatSystemLine(client.nickname + " left the chat"));
    }
    ClientSender sender = client.sender;
    if (sender != null) {
      sender.shutdown();
    }
    Thread listenerThread = client.listenerThread;
    if (listenerThread != null) {
      listenerThread.interrupt();
    }
    Thread senderThread = client.senderThread;
    if (senderThread != null) {
      senderThread.interrupt();
    }
  }

  @Override
  public void close() throws IOException {
    if (!closed.compareAndSet(false, true)) {
      return;
    }
    started.set(false);
    ServerDispatcher currentDispatcher = dispatcher;
    if (currentDispatcher != null) {
      currentDispatcher.shutdown();
    }
    ServerSocket currentServerSocket = serverSocket;
    if (currentServerSocket != null && !currentServerSocket.isClosed()) {
      currentServerSocket.close();
    }
    if (acceptThread != null) {
      acceptThread.interrupt();
    }
    List<Client> snapshot = new ArrayList<>(clients);
    for (Client client : snapshot) {
      disconnect(client, false);
    }
    writeDeadlineScheduler.shutdownNow();
    try {
      writeDeadlineScheduler.awaitTermination(5, TimeUnit.SECONDS);
    } catch (InterruptedException ignored) {
      Thread.currentThread().interrupt();
    }
  }

  private void acceptLoop() {
    while (!closed.get()) {
      try {
        Socket socket = serverSocket.accept();
        socket.setTcpNoDelay(true);
        registerClient(socket);
      } catch (SocketException ignored) {
        if (closed.get()) {
          break;
        }
      } catch (IOException e) {
        if (!closed.get()) {
          log.println("[Server] Accept error: " + e.getMessage());
        }
      }
    }
  }

  private void registerClient(Socket socket) throws IOException {
    int id = nextClientNumber.getAndIncrement();
    Client client = new Client(id, socket, "");
    boolean added = false;
    try {
      String nickname = reserveInitialNickname(id, client);
      client.nickname = nickname;
      ClientSender sender = new ClientSender(
          client,
          clientQueueCapacity,
          writeDeadlineScheduler,
          writeTimeout,
          () -> closeClientSocket(client),
          () -> disconnect(client, true));
      client.sender = sender;
      clients.add(client);
      added = true;
      client.senderThread = Thread.startVirtualThread(sender);
      client.listenerThread = Thread.startVirtualThread(new ClientListener(client, dispatcher));
      sendToClient(client, "Welcome, " + nickname + "!");
    } catch (IOException | RuntimeException e) {
      if (added) {
        clients.remove(client);
      }
      releaseNickname(client);
      closeClientSocket(client);
      ClientSender sender = client.sender;
      if (sender != null) {
        sender.shutdown();
      }
      throw e;
    }
  }

  private String reserveInitialNickname(int id, Client client) {
    synchronized (registryLock) {
      int candidate = id;
      while (true) {
        String nickname = "Client-" + candidate;
        if (!nicknames.containsKey(nickname)) {
          nicknames.put(nickname, client);
          return nickname;
        }
        candidate = nextClientNumber.getAndIncrement();
      }
    }
  }

  private void sendWho(Client requester) {
    String list;
    synchronized (registryLock) {
      list = String.join(", ", nicknames.keySet().stream().sorted().toList());
    }
    sendToClient(requester, "Online: " + list);
  }

  private void renameClient(Client client, String command) {
    String[] parts = command.split("\\s+", 2);
    if (parts.length < 2 || !isValidNickname(parts[1])) {
      sendToClient(client, "Invalid nickname. Use /nick <letters-or-digits>");
      return;
    }
    String newNickname = parts[1].trim();
    String oldNickname;
    synchronized (registryLock) {
      oldNickname = client.nickname;
      Client owner = nicknames.get(newNickname);
      if (owner != null && owner != client) {
        sendToClient(client, "Nickname already in use.");
        return;
      }
      if (newNickname.equals(oldNickname)) {
        sendToClient(client, "You are already " + newNickname + ".");
        return;
      }
      nicknames.remove(oldNickname);
      nicknames.put(newNickname, client);
      client.nickname = newNickname;
    }
    sendToClient(client, "Nickname changed to " + newNickname + ".");
    broadcastToOthers(client, formatSystemLine(oldNickname + " is now known as " + newNickname));
  }

  private boolean isValidNickname(String nickname) {
    String trimmed = nickname.trim();
    if (trimmed.isEmpty() || trimmed.length() > 24 || trimmed.startsWith("/")) {
      return false;
    }
    for (int i = 0; i < trimmed.length(); i++) {
      char ch = trimmed.charAt(i);
      if (!(Character.isLetterOrDigit(ch) || ch == '-' || ch == '_' || ch == '.')) {
        return false;
      }
    }
    return true;
  }

  private void releaseNickname(Client client) {
    synchronized (registryLock) {
      String current = client.nickname;
      if (nicknames.get(current) == client) {
        nicknames.remove(current);
      }
    }
  }

  private void sendToClient(Client client, String message) {
    ClientSender sender = client.sender;
    if (sender == null || !sender.sendMessage(message)) {
      disconnect(client, false);
    }
  }

  private void broadcastToOthers(Client source, String message) {
    List<Client> snapshot = new ArrayList<>(clients);
    for (Client target : snapshot) {
      if (target != source) {
        sendToClient(target, message);
      }
    }
  }

  private String formatChatLine(String name, String text) {
    String time = LocalTime.now().format(TIME_FORMATTER);
    ChatMessage message = new ChatMessage(time, name, text);
    return "[" + message.time() + "] " + message.name() + ": " + message.text();
  }

  private String formatSystemLine(String text) {
    String time = LocalTime.now().format(TIME_FORMATTER);
    return "[" + time + "] " + text;
  }

  private void closeClientSocket(Client client) {
    try {
      if (!client.socket.isClosed()) {
        client.socket.close();
      }
    } catch (IOException ignored) {
    }
  }

  private void cleanupFailedStart() {
    ServerDispatcher currentDispatcher = dispatcher;
    if (currentDispatcher != null) {
      currentDispatcher.shutdown();
    }
    ServerSocket currentServerSocket = serverSocket;
    if (currentServerSocket != null) {
      try {
        if (!currentServerSocket.isClosed()) {
          currentServerSocket.close();
        }
      } catch (IOException ignored) {
      }
    }
    Thread currentAcceptThread = acceptThread;
    if (currentAcceptThread != null) {
      currentAcceptThread.interrupt();
    }
  }
}
