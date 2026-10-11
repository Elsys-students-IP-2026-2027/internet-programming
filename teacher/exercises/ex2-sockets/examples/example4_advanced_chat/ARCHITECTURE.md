# Architecture of a Multi-Client TCP Chat

The Java TCP chat application uses an **asynchronous architecture** with separate threads handling different tasks. In the current implementation, each client's incoming and outgoing messages are handled by **virtual threads**, so a slow client does not block communication with the other users.

Inactive connections are handled using a **5-minute socket timeout** and a **keep-alive mechanism**.

---

## Server Architecture

### AdvancedChatServer

The main server class is responsible for accepting new client connections.

- Opens a `ServerSocket` on port **2002**.
- Starts the central `ServerDispatcher` thread.
- Continuously accepts client connections using `accept()`.
- For each new client:
    - Creates a `Client` object.
    - Starts two virtual threads: `ClientListener` and `ClientSender`.
    - Registers the client with `ServerDispatcher`.

### ServerDispatcher

The dispatcher manages connected clients and distributes messages.

- Maintains a list of active clients (`mClients`).
- Maintains a queue of incoming messages (`mMessageQueue`).
- Waits using `wait()` when the message queue is empty.
- When a new message arrives:
    - Adds it to the queue.
    - Calls `notify()` to wake up the waiting thread.
    - Forwards the message to each client's outgoing message queue.
- Broadcasts every message to all connected clients.

### ClientListener

Each client has a listener that handles incoming messages.

- Reads incoming data from the client's socket.
- Reads messages line by line using `BufferedReader.readLine()`.
- Uses `setSoTimeout()` with a timeout of 5 minutes to detect periods without incoming data.
- Sends a `!keep-alive` message when a timeout occurs.
- Forwards received messages to `ServerDispatcher`.
- When the connection closes:
    - Stops the corresponding `ClientSender`.
    - Removes the client from the active client list.

### ClientSender

Each client has a separate sender responsible for outgoing messages.

- Maintains a private message queue for the client.
- Waits for new messages using `wait()`.
- Sends messages through `PrintWriter` asynchronously.
- Prevents a slow client from blocking message delivery to other clients.
- Uses `notify()` to signal that new messages are available.

### Client

The `Client` class stores the data associated with a connection.

It contains:

- The socket reference (`mSocket`).
- The listener thread (`mClientListener`).
- The sender thread (`mClientSender`).
- The client's ID (`mClientId`).

---

## Client Architecture

### AdvancedChatClient

The main client class connects to the server and handles communication.

- Connects to the server at `localhost:2002`.
- Starts two separate virtual threads through `TextDataTransmitter` to handle incoming and outgoing data.

### TextDataTransmitter

This class handles communication between the console and the server. It uses two separate threads.

**Thread 1: Socket → Console (Receiving)**

- Continuously reads data from the socket.
- Prints received messages to the console.
- Filters out internal `!keep-alive` messages so they are not displayed.

**Thread 2: Console → Socket (Sending)**

- Reads lines entered by the user.
- Sends them to the server through the socket.
- Stops when the user enters the `EXIT` command.

---

## Message Flow

```text
Client A sends "Hello"
        ↓
ClientListener (A) reads the message
        ↓
ServerDispatcher.dispatchMessage() adds it to the queue
        ↓
ServerDispatcher.run() forwards it to all ClientSender instances
        ↓
ClientSender (A) adds it to its outgoing queue
ClientSender (B) adds it to its outgoing queue
        ↓
Each ClientSender sends the message to its client
        ↓
TextDataTransmitter (A) reads and prints the message
TextDataTransmitter (B) reads and prints the message
```

---

## Key Mechanisms

### 1. Synchronization

- Shared data, including message queues and the client list, is protected using `synchronized` methods.
- The implementation relies on Java's built-in monitor synchronization rather than explicit `Lock` objects.

### 2. Wait and Notify

- `wait()` pauses a thread while it waits for new messages.
- `notify()` wakes a waiting thread when new messages become available.
- This avoids busy-waiting and reduces unnecessary CPU usage.

### 3. Keep-Alive Mechanism

- After 5 minutes without incoming data, the socket read operation times out.
- The server sends a `!keep-alive` message to the client.
- If the connection has been lost, the server can detect the failure and close the connection when an I/O error occurs.

This helps prevent dead connections from remaining in the active client list.

### 4. Asynchronous Message Delivery

- Each client has its own outgoing message queue.
- A slow client does not directly block message delivery to other clients.
- Each `ClientSender` processes its queue independently.

---

## How to Run the Application

### 1. Start the server

Open the first terminal:

```bash
cd out
javac ../teacher/exercises/ex2-sockets/examples/example4_advanced_chat/*.java
java AdvancedChatServer
```

### 2. Start the first client

Open a second terminal:

```bash
cd out
java AdvancedChatClient
```

### 3. Start another client

Open a third terminal:

```bash
cd out
java AdvancedChatClient
```

You can open additional terminals to connect more clients to the server.

---

## Advantages

- **Scalability:** Virtual threads make it practical to handle many concurrent client connections, although actual capacity depends on the implementation and system resources.
- **Responsiveness:** A slow client is less likely to affect message delivery to other clients.
- **Connection handling:** Socket timeouts help detect inactive connections and I/O failures.
- **CPU efficiency:** `wait()` and `notify()` avoid busy-waiting when message queues are empty.
- **Easy to extend:** Features such as usernames, chat groups, and message history can be added later.

---

## Limitations

- Each client uses separate threads for receiving and sending data, in addition to the main application thread.
- Performance can become an issue with a large number of clients, depending on resource usage and the implementation.
- For applications requiring more control over network I/O, Java NIO and non-blocking I/O are worth considering.
- Messages are not stored persistently. If a client disconnects, it cannot retrieve messages sent while it was offline.
