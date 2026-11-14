package com.grahambartley.hardwired.screen;

import com.grahambartley.hardwired.network.ExchangeViewPayload;
import com.grahambartley.hardwired.network.PhoneSaidPayload;
import com.grahambartley.hardwired.network.PhoneViewPayload;
import net.minecraft.client.MinecraftClient;

public final class PhoneScreens {

  private PhoneScreens() {}

  public static void show(MinecraftClient client, PhoneViewPayload view) {
    if (client.currentScreen instanceof DialScreen screen && screen.shows(view.phone())) {
      screen.accept(view);
    } else {
      client.setScreen(new DialScreen(view));
    }
  }

  public static void hear(MinecraftClient client, PhoneSaidPayload said) {
    client.inGameHud.getChatHud().addMessage(said.line());
    if (client.currentScreen instanceof DialScreen screen && screen.shows(said.phone())) {
      screen.hear(said.line());
    }
  }

  public static void showExchange(MinecraftClient client, ExchangeViewPayload view) {
    if (client.currentScreen instanceof ExchangeScreen screen && screen.shows(view.exchange())) {
      screen.accept(view);
    } else {
      client.setScreen(new ExchangeScreen(view));
    }
  }
}
