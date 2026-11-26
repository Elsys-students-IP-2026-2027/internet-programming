# Multipart Lab – упражнение за качване на файлове и HTTP версии


## Стартиране

```bash
./gradlew run          # UploadServer → http://localhost:8080
./gradlew probe        # HttpProbe – коя HTTP версия реално се използва
./gradlew probe -Purls=https://example.com,http://localhost:8080/info
```

## Структура

| Файл | Статус                                                               |
|------|----------------------------------------------------------------------|
| `src/main/resources/index.html` | готов файл с 3 форми (правилна, без `enctype`, `fetch` + `FormData`) |
| `upload/httpprobe/HttpProbe.java` | **Задача 0 в началото правим заедно за HTTP версиите**               |
| `upload/Part.java`, `MimeTypes.java`, `MultipartParser.java` | готови помощни класове                                               |
| `upload/UploadServer.java` | **Задачи 1 и 2 в TODO**                                              |
| `upload/httpprobe/AltSvc.java` | готов                                                                |

## Задача 0 – `HttpProbe`

Изпращаме заявки с предпочитана версия HTTP/1.1 и HTTP/2 и сравняваме с `response.version()`.
Клиентът само **предлага** версия – реалната се договаря със сървъра (при HTTPS чрез ALPN по време на TLS handshake).

**Къде се конфигурира версията?** На сървърната страна – web сървър, reverse proxy, load balancer, CDN:
- **Nginx, Apache, Caddy** – HTTP/1.1, HTTP/2, HTTP/3 (Nginx и Caddy поддържат HTTP/3 върху QUIC);
- **Cloudflare и други CDN** – HTTP/2 и HTTP/3 се включват от настройките;
- **Tomcat** – най-много HTTP/2 (не поддържа HTTP/3).

В Spring Boot (`application.properties`):
```properties
server.http2.enabled=true
# браузърите използват HTTP/2 САМО през HTTPS, нужен е и TLS:
server.ssl.enabled=true
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-password=changeit
```
Без TLS остава само `h2c` (HTTP/2 без криптиране) – браузърите не го поддържат.
Затова нашият `UploadServer` (`http://`) винаги отговаря с HTTP/1.1.

**Gateway / reverse proxy:** версията се договаря **поотделно за всяка връзка**:
```
браузър ──HTTP/3 или HTTP/2──▶ Nginx / Cloudflare / API Gateway ──HTTP/1.1──▶ Tomcat (Spring Boot)
```
Така клиентът може да ползва HTTP/3, дори бекендът да поддържа само HTTP/1.1.

## Задача 1 – `POST /upload` (TODO 1.1 – 1.9)

| Проверка | Отговор |
|----------|---------|
| метод ≠ POST | `405` + `Allow: POST` |
| Content-Type ≠ `multipart/form-data` | `415` |
| липсва boundary / грешно тяло | `400` |
| тяло > 1 MB | `413` |
| липсва `student` или `homework` | `400` |
| тип по разширение не е разрешен | `415` |
| magic bytes не съвпадат с разширението | `422` |
| всичко е наред | `201 Created` + `Location: /files/...` + JSON |

Пробвайте в DevTools - Network:
1. Правилната форма – разгледайте Request Payload и `boundary`.
2. Формата без `enctype` – какъв `Content-Type` изпраща браузърът? Какво получава сървърът?
3. `.txt` файл, преименуван на `.pdf` - трябва `422`.

С curl:
```bash
curl -i -F student="Иван" -F homework=@notes.txt http://localhost:8080/upload
curl -i -X GET http://localhost:8080/upload                       # 405
curl -i -d "student=Иван" http://localhost:8080/upload             # 415
```

## Задача 2 – `GET /files/{name}` (TODO 2.1 – 2.2)

- Правилен `Content-Type` по разширението (PDF да се отваря в браузъра, не да се сваля).
- `?download` - `Content-Disposition: attachment`.

## Бенчмарк (по желание)

Стартирайте сървъра (`./gradlew run`) и в друг терминал:

```bash
# ab – идва с macOS (част от Apache): 1000 заявки, 50 едновременно
ab -n 1000 -c 50 http://localhost:8080/info

# hey (brew install hey): 1000 заявки, 50 едновременно
hey -n 1000 -c 50 http://localhost:8080/info

# wrk (brew install wrk): 10 секунди, 4 нишки, 50 връзки
wrk -t4 -c50 -d10s http://localhost:8080/info

# качване на файл с curl, 20 пъти – общо време
time (for i in $(seq 20); do curl -s -o /dev/null -F student=Ivan -F homework=@notes.txt http://localhost:8080/upload; done)
```

Гледайте: **Requests/sec**, средна **latency** и броя грешки (non-2xx).

### Windows (PowerShell)

Сървърът: `.\gradlew.bat run`

```powershell
# ab – от Apache за Windows (https://www.apachelounge.com/download/ - bin\ab.exe)
#      или ако имате XAMPP: C:\xampp\apache\bin\ab.exe
ab.exe -n 1000 -c 50 http://localhost:8080/info

# hey – изтеглете hey_windows_amd64 от https://github.com/rakyll/hey/releases (преименувайте на hey.exe)
#       или с Go: go install github.com/rakyll/hey@latest
hey.exe -n 1000 -c 50 http://localhost:8080/info

# wrk – няма версия за Windows - през WSL (Ubuntu): sudo apt install wrk
wrk -t4 -c50 -d10s http://localhost:8080/info

# качване на файл 20 пъти – общо време (curl.exe е вграден в Windows 10/11)
Measure-Command { 1..20 | ForEach-Object { curl.exe -s -o NUL -F student=Ivan -F homework=@notes.txt http://localhost:8080/upload } }
```
> В PowerShell пишете `curl.exe`, не `curl` – `curl` е псевдоним на `Invoke-WebRequest`.



