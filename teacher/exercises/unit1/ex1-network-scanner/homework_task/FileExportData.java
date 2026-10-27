package homework_task;

public record FileExportData(String ip, boolean reachable, String hostname, String mac) {

  @Override
  public String toString() {
    return String.format("%-15s | %-9s | %-20s | %s",
        ip, reachable ? "YES" : "NO", hostname, mac);
  }
}

