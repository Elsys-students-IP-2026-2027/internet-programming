package bg.tues.chat;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

final class ServerDispatcher implements Runnable {
  private final ChatServer server;
  private final BlockingQueue<Event> queue;
  private final AtomicBoolean running = new AtomicBoolean(true);
  private Thread dispatcherThread;

  ServerDispatcher(ChatServer server, int queueCapacity) {
    this.server = server;
    this.queue = new ArrayBlockingQueue<>(queueCapacity);
  }

  void start() {
    dispatcherThread = Thread.startVirtualThread(this);
  }

  boolean dispatchMessage(Client client, String message) {
    return queue.offer(new Event(EventType.MESSAGE, client, message, false));
  }

  void requestDisconnect(Client client, boolean announce) {
    if (!queue.offer(new Event(EventType.DISCONNECT, client, null, announce))) {
      server.disconnect(client, announce);
    }
  }

  void shutdown() {
    if (running.compareAndSet(true, false)) {
      queue.offer(new Event(EventType.SHUTDOWN, null, null, false));
      if (dispatcherThread != null) {
        dispatcherThread.interrupt();
      }
    }
  }

  @Override
  public void run() {
    while (running.get() || !queue.isEmpty()) {
      try {
        Event event = queue.take();
        if (event.type == EventType.MESSAGE) {
          server.handleClientMessage(event.client, event.message);
        } else if (event.type == EventType.DISCONNECT) {
          server.disconnect(event.client, event.announce);
        } else if (event.type == EventType.SHUTDOWN) {
          break;
        }
      } catch (InterruptedException ignored) {
        Thread.currentThread().interrupt();
        break;
      }
    }
  }

  private record Event(EventType type, Client client, String message, boolean announce) {
  }

  private enum EventType {MESSAGE, DISCONNECT, SHUTDOWN}
}
