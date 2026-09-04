package student.exercises.ex1_network_scanner;

import java.net.SocketException;

/**
 * Задача 1: Мрежов скенер на интерфейси.
 *
 * Обходете всички мрежови интерфейси на машината и за всеки изведете:
 *   - име и описание (getName / getDisplayName)
 *   - MAC адрес във формат AA-BB-CC-... (getHardwareAddress) — внимание: може да е null
 *   - всички IP адреси, IPv4 и IPv6 (getInetAddresses)
 *   - дали интерфейсът е активен (isUp)
 *   - дали поддържа multicast (supportsMulticast)
 *   - дали е loopback (isLoopback)
 */
public class NetworkScanner {

    public static void main(String[] args) throws SocketException {
        // TODO 1: Вземете всички интерфейси с NetworkInterface.getNetworkInterfaces()

        // TODO 2: Обходете ги (Enumeration: hasMoreElements / nextElement)

        // TODO 3: Изведете име и описание на всеки интерфейс

        // TODO 4: Прочетете MAC адреса (getHardwareAddress) и го форматирайте AA-BB-CC-...
        //         Внимавайте за null.

        // TODO 5: Обходете IP адресите (getInetAddresses) и ги изведете

        // TODO 6: Изведете isUp(), supportsMulticast(), isLoopback()

        // TODO 7: Обработете SocketException коректно
    }
}
