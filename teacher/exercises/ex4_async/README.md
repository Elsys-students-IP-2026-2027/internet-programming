# ex4_async — Асинхронни сокети в Java (NIO / NIO.2)

Примери към урока **„Сокети, NIO, NIO.2, Async"** (презентация
`Unit1_Lesson_15_16_Сокети_NIO_NIO2_Async`).

Папките следват **реда на презентацията** 

Всички примери са echo-сървъри/клиенти и наследяват `common.Demo`
(споделен `Logger` + `sleep()`), за да е еднакъв изходът при демонстрация.

## Компилиране

```bash
cd teacher/exercises/ex4_async
find src -name "*.java" > sources.txt
javac -d out @sources.txt
```

Стартиране на клас (пример):

```bash
java -cp out nio_channel.NioBlockingEchoServer
```
Стартирането може д астав аи от IDE-to
Повечето примери са двойка **сървър + клиент** — пуснете сървъра в един
терминал, клиента в друг.

---

## Ред 

### `common` — обща база
- `Demo` — базов клас с `Logger` и `sleep()`; всички демота го наследяват.

### `nio_channel` — блокиращо и неблокиращо I/O (слайдове 3–18)
Прогресия от класическо блокиращо към неблокиращо `SocketChannel`.
- `NioBlockingEchoServer` — блокиращ `ServerSocketChannel` (отправна точка).
- `NioNonBlockingEchoServer` — `configureBlocking(false)`, busy-loop.
- `NioChannelEchoClient` — общ клиент за NIO сървърите.

### `nio_selector` — мултиплексиране със `Selector` (слайдове 19–27)
Един поток обслужва много връзки чрез `Selector` (event loop на NIO).
- `NioSelectorEchoServer` — базов `Selector` цикъл.
- `NioMultiplexingEchoServer` — пълен мултиплексиращ сървър (OP_ACCEPT/READ/WRITE).
- Клиент: `nio_channel.NioChannelEchoClient`.

### `nio2_future` — NIO.2 с `Future` (слайдове 28–40)
`AsynchronousServerSocketChannel` + `Future` (polling / блокиращ `get()`).
- `Nio2EchoFutureServer` — single-thread вариант.
- `Nio2EchoFutureServerThreadPool` — с `ExecutorService`.
- `Nio2EchoFutureClient` — клиент.

### `nio2_completion_handler` — NIO.2 с `CompletionHandler` (слайдове 41–47)
Callback-базиран стил (`completed()` / `failed()`) — истинска асинхронност.
- `server.Nio2CompletionHandlerEchoServer` — сървър.
- `client.Nio2CompletionHandlerEchoClient` — клиент.
- Accept/Read/WriteCompletionHandler + Attachment — помощни handler-и.
> Сървърът и клиентът имат собствени handler-и със съвпадащи имена → държат се в
> отделни под-пакети (`server` / `client`), затова не ги сливаме.

### `nio2_group` — `AsynchronousChannelGroup` (слайдове 38–39)
Споделен thread pool за няколко асинхронни канала.
- `AsyncServerWithGroup` — сървър със собствена channel group.
- `AsyncGroupClient` — клиент.

### `patterns_reactor` — шаблон Reactor (разширено)
Синхронен демултиплексор (стилът зад `Selector`).
- `ReactorInitiator` — входна точка.
- `EchoClient` — общ клиент за Reactor и Proactor демотата.

### `patterns_proactor` — шаблон Proactor (разширено)
Асинхронно завършване (стилът зад `CompletionHandler`).
- `ProactorInitiator` — входна точка (клиент: `patterns_reactor.EchoClient`).

### `nio2_file` — асинхронен файлов I/O (слайд 35)
`AsynchronousFileChannel` — същата идея, но върху файл вместо сокет.
- `AsyncFileServer` / `AsyncFileClient`.
- `assignment2.txt` — примерен вход.

### `multicast` — UDP multicast (слайдове 34, 91–93)
- `MulticastSender` — праща към група.
- `MulticastReceiver` — присъединява се към групата и получава.

### `deadlock` 
- `DeadlockExample` — класически deadlock (защо редът на заключване има значение).

