# Chat module architecture (`bg.tues.chat`)

## Основни класове

- `ChatServer` — приема връзки, регистрира клиенти, управлява nicknames и lifecycle (`start/getPort/close`).
- `ServerDispatcher` — централна **ограничена** опашка за входящи събития; обработва съобщения/дисконекти извън I/O нишките.
- `ClientListener` — чете UTF-8 редове от един клиент, филтрира празни, обработва `EXIT` и `/quit`.
- `ClientSender` — отделна виртуална нишка и **ограничена** изходяща опашка за всеки клиент.
- `ChatClient` — конзолен клиент.
- `ChatMessage` — JSON record (`toJson/fromJson`) чрез Gson.

## Команди

- `/who` → връща онлайн nickname-и само към заявилия клиент.
- `/nick <name>` → атомично преименуване; при невалидно/заето име старото се запазва.
- `EXIT` или `/quit` → прекъсва връзката без broadcast на самата команда.

## Неблокиращо писане и timeout

- Никога не се пише към сокет от dispatcher/scheduler/заключвания.
- Всеки `ClientSender` пише самостоятелно и по ред.
- За всяко писане се планира deadline чрез общ `ScheduledExecutorService`.
- При timeout scheduler затваря **socket-а** (не прекъсва writer нишката), което отблокира блокирал `flush()/write()`.
- При препълнена клиентска опашка клиентът се откача, за да не блокира останалите.
- Ако scheduler е в shutdown, `ClientSender` продължава с директно писане (без нов deadline), за да не губи вече взетото съобщение.

## Lifecycle детайли

- Регистрацията на клиент добавя клиента в активния set **преди** listener нишката, за да няма leak при ранно разкачане.
- При частичен fail в `start()`/`registerClient()` ресурсите се чистят и nickname-ът се освобождава атомично.

## Стартиране

От `teacher/exercises/ex5-gradle-maven/solution-gradle`:

```bash
./gradlew :chat:run
./gradlew :chat:runClient --no-daemon --console=plain -PclientArgs="localhost 2002"
```

