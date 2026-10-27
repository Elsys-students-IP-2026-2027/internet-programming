package bg.tues.chat;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

final class ClientSender implements Runnable {
  private final Client client;
  private final BlockingQueue<String> messageQueue;
  private final ScheduledExecutorService scheduler;
  private final Duration writeTimeout;
  private final Runnable forceCloseSocket;
  private final Runnable onFailure;
  private final BufferedWriter writer;
  private final AtomicBoolean stopped = new AtomicBoolean(false);

  ClientSender(
      Client client,
      int queueCapacity,
      ScheduledExecutorService scheduler,
      Duration writeTimeout,
      Runnable forceCloseSocket,
      Runnable onFailure) throws IOException {
    this(
        client,
        queueCapacity,
        scheduler,
        writeTimeout,
        client.socket.getOutputStream(),
        forceCloseSocket,
        onFailure);
  }

  ClientSender(
      Client client,
      int queueCapacity,
      ScheduledExecutorService scheduler,
      Duration writeTimeout,
      OutputStream outputStream,
      Runnable forceCloseSocket,
      Runnable onFailure) {
    this.client = client;
    this.messageQueue = new ArrayBlockingQueue<>(queueCapacity);
    this.scheduler = scheduler;
    this.writeTimeout = writeTimeout;
    this.forceCloseSocket = forceCloseSocket;
    this.onFailure = onFailure;
    this.writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
  }

  boolean sendMessage(String message) {
    if (stopped.get()) {
      return false;
    }
    return messageQueue.offer(message);
  }

  void shutdown() {
    if (stopped.compareAndSet(false, true)) {
      forceCloseSocket.run();
    }
  }

  @Override
  public void run() {
    try {
      while (!stopped.get() && !Thread.currentThread().isInterrupted()) {
        String message = messageQueue.take();
        writeWithDeadline(message);
      }
    } catch (InterruptedException ignored) {
      Thread.currentThread().interrupt();
    } catch (IOException ignored) {
      onFailure.run();
    } finally {
      stopWriter();
    }
  }

  private void writeWithDeadline(String message) throws IOException {
    AtomicBoolean completed = new AtomicBoolean(false);
    ScheduledFuture<?> timeoutTask = null;
    try {
      timeoutTask = scheduler.schedule(
          () -> {
            if (completed.compareAndSet(false, true)) {
              forceCloseSocket.run();
            }
          },
          writeTimeout.toMillis(),
          TimeUnit.MILLISECONDS);
    } catch (RejectedExecutionException ignored) {
      if (stopped.get()) {
        throw new IOException("Sender is shutting down.");
      }
    }
    try {
      writer.write(message);
      writer.newLine();
      writer.flush();
      completed.set(true);
    } finally {
      if (timeoutTask != null) {
        timeoutTask.cancel(false);
      }
    }
  }

  private void stopWriter() {
    stopped.set(true);
    forceCloseSocket.run();
    try {
      writer.close();
    } catch (IOException ignored) {
    }
  }
}
