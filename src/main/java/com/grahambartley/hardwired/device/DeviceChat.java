package com.grahambartley.hardwired.device;

import java.util.Optional;
import net.minecraft.network.message.ChatVisibility;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class DeviceChat {

  private static final int MIN_TICKS_BETWEEN_LINES = 20;

  private final ActionRate rate = new ActionRate(MIN_TICKS_BETWEEN_LINES);

  public Optional<String> accept(ServerPlayerEntity player, String raw) {
    if (player.getClientChatVisibility() == ChatVisibility.HIDDEN) {
      player.sendMessage(
          Text.translatable("chat.disabled.options").formatted(Formatting.RED), true);
      return Optional.empty();
    }
    String line = IntercomLine.clean(raw);
    if (line.isEmpty()) {
      return Optional.empty();
    }
    if (!rate.allow(player.getUuid(), player.getServerWorld().getTime())) {
      player.sendMessage(Text.translatable("device.hardwired.too_fast"), true);
      return Optional.empty();
    }
    return Optional.of(line);
  }

  public void forget(ServerPlayerEntity player) {
    rate.forget(player.getUuid());
  }

  public static boolean reads(ServerPlayerEntity listener) {
    return listener.getClientChatVisibility() == ChatVisibility.FULL;
  }
}
