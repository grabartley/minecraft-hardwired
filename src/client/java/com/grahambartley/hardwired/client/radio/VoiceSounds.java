package com.grahambartley.hardwired.client.radio;

import com.grahambartley.hardwired.block.SoundingBlockEntity;
import com.grahambartley.hardwired.config.SyncedServerConfig;
import com.grahambartley.hardwired.radio.ReceptionMix;
import com.grahambartley.hardwired.radio.VoiceJitter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

final class VoiceSounds {

  private static final int RETRY_TICKS = 20;

  private static final Map<SoundingBlockEntity, Playing> PLAYING = new HashMap<>();

  private record Playing(VoiceSoundInstance sound, BlockPos desk, VoiceJitter tap, long retryAt) {

    boolean plays(@Nullable BlockPos station) {
      return desk.equals(station);
    }
  }

  private VoiceSounds() {}

  static void update(MinecraftClient client, Set<SoundingBlockEntity> voices) {
    Iterator<Map.Entry<SoundingBlockEntity, Playing>> playing = PLAYING.entrySet().iterator();
    while (playing.hasNext()) {
      Map.Entry<SoundingBlockEntity, Playing> entry = playing.next();
      if (!voices.contains(entry.getKey()) || !entry.getValue().plays(entry.getKey().station())) {
        stop(client, entry.getValue());
        playing.remove();
      }
    }
    voices.forEach(device -> keepPlaying(client, device));
  }

  static void forgetAll(MinecraftClient client) {
    PLAYING.values().forEach(playing -> stop(client, playing));
    PLAYING.clear();
  }

  private static void keepPlaying(MinecraftClient client, SoundingBlockEntity device) {
    long now = client.world.getTime();
    Playing current = PLAYING.get(device);
    if (current != null
        && (client.getSoundManager().isPlaying(current.sound()) || now < current.retryAt())) {
      return;
    }
    if (current != null) {
      stop(client, current);
    }
    BlockPos desk = device.station();
    VoiceJitter tap = VoiceFeed.attach(desk);
    VoiceSoundInstance sound =
        new VoiceSoundInstance(
            tap,
            device.getPos(),
            device.hearing(SyncedServerConfig.get().radio()),
            desk.asLong(),
            () -> ReceptionMix.stationLoudness(device.volume(), device.strength()));
    PLAYING.put(device, new Playing(sound, desk, tap, now + RETRY_TICKS));
    client.getSoundManager().play(sound);
  }

  private static void stop(MinecraftClient client, Playing playing) {
    playing.sound().silence();
    client.getSoundManager().stop(playing.sound());
    VoiceFeed.detach(playing.desk(), playing.tap());
  }
}
