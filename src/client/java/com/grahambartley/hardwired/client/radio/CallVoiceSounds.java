package com.grahambartley.hardwired.client.radio;

import com.grahambartley.hardwired.config.ClientSettingsStore;
import com.grahambartley.hardwired.network.CallVoicePayload;
import com.grahambartley.hardwired.radio.VoiceJitter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

public final class CallVoiceSounds {

  private record Playing(CallVoiceSoundInstance sound, VoiceJitter tap) {}

  private static final Map<BlockPos, Playing> PLAYING = new HashMap<>();

  private CallVoiceSounds() {}

  public static void register() {
    ClientPlayNetworking.registerGlobalReceiver(
        CallVoicePayload.ID, (payload, context) -> receive(context.client(), payload));
    ClientTickEvents.END_CLIENT_TICK.register(CallVoiceSounds::tick);
    ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> forgetAll(client));
  }

  static int playing() {
    return PLAYING.size();
  }

  private static void receive(MinecraftClient client, CallVoicePayload frame) {
    if (!ClientSettingsStore.radio().hearVoiceCalls()) {
      return;
    }
    BlockPos phone = frame.phone();
    VoiceFeed.receive(phone, frame.adpcm());
    if (!PLAYING.containsKey(phone)) {
      VoiceJitter tap = VoiceFeed.attach(phone);
      CallVoiceSoundInstance sound = new CallVoiceSoundInstance(tap, phone.asLong());
      PLAYING.put(phone, new Playing(sound, tap));
      client.getSoundManager().play(sound);
    }
  }

  private static void tick(MinecraftClient client) {
    Iterator<Map.Entry<BlockPos, Playing>> playing = PLAYING.entrySet().iterator();
    while (playing.hasNext()) {
      Map.Entry<BlockPos, Playing> entry = playing.next();
      if (!VoiceFeed.onAir(entry.getKey()) || !ClientSettingsStore.radio().hearVoiceCalls()) {
        stop(client, entry.getKey(), entry.getValue());
        playing.remove();
      }
    }
  }

  private static void forgetAll(MinecraftClient client) {
    PLAYING.forEach((phone, playing) -> stop(client, phone, playing));
    PLAYING.clear();
  }

  private static void stop(MinecraftClient client, BlockPos phone, Playing playing) {
    playing.sound().silence();
    client.getSoundManager().stop(playing.sound());
    VoiceFeed.detach(phone, playing.tap());
  }
}
