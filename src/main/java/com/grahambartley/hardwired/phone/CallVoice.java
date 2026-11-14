package com.grahambartley.hardwired.phone;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.network.CallVoicePayload;
import com.grahambartley.hardwired.radio.VoiceRateLimit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Util;

public final class CallVoice {

  private static final Map<UUID, VoiceRateLimit> LIMITS = new HashMap<>();

  private CallVoice() {}

  public static void frame(ServerPlayerEntity player, CallVoicePayload frame) {
    for (UUID id : audience(player, frame)) {
      ServerPlayerEntity listener = player.getServer().getPlayerManager().getPlayer(id);
      if (listener != null && ServerPlayNetworking.canSend(listener, CallVoicePayload.ID)) {
        ServerPlayNetworking.send(listener, frame);
      }
    }
  }

  public static List<UUID> audience(ServerPlayerEntity player, CallVoicePayload frame) {
    long phone = frame.phone().asLong();
    Optional<Call> call = Switchboard.of(player.getServerWorld()).callOf(phone);
    List<UUID> heard =
        CallAudience.of(
            call.map(Call::members).orElse(Map.of()),
            call.filter(Call::isConnected).isPresent(),
            phone,
            player.getUuid(),
            Hardwired.SERVER_CONFIG.phone().voiceCalls(),
            frame.adpcm().length);
    long now = Util.getMeasuringTimeMs();
    if (heard.isEmpty()
        || !LIMITS.computeIfAbsent(player.getUuid(), id -> new VoiceRateLimit(now)).allows(now)) {
      return List.of();
    }
    return heard;
  }

  public static void clear() {
    LIMITS.clear();
  }

  public static void forget(ServerPlayerEntity player) {
    LIMITS.remove(player.getUuid());
  }
}
