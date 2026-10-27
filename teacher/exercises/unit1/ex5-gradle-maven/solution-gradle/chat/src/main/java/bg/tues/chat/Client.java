package bg.tues.chat;

import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

final class Client {
  final int id;
  final Socket socket;
  final AtomicBoolean disconnected = new AtomicBoolean(false);
  volatile String nickname;
  volatile Thread listenerThread;
  volatile Thread senderThread;
  volatile ClientSender sender;

  Client(int id, Socket socket, String nickname) {
    this.id = id;
    this.socket = socket;
    this.nickname = nickname;
  }
}
