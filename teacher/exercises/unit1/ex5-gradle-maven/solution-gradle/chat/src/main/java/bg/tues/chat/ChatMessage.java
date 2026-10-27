package bg.tues.chat;

import com.google.gson.Gson;

public record ChatMessage(String time, String name, String text) {
  private static final Gson GSON = new Gson();

  public String toJson() {
    return GSON.toJson(this);
  }

  public static ChatMessage fromJson(String json) {
    return GSON.fromJson(json, ChatMessage.class);
  }
}
