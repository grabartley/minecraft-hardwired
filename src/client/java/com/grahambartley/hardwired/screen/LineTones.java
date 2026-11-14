package com.grahambartley.hardwired.screen;

import com.grahambartley.hardwired.config.ClientSettingsStore;
import com.grahambartley.hardwired.phone.LineStatus;
import com.grahambartley.hardwired.phone.RingCadence;
import com.grahambartley.hardwired.registry.ModSounds;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;

final class LineTones {

  enum Tone {
    NONE,
    DIAL,
    RINGBACK,
    BUSY
  }

  private static final int BUSY_TICKS = 10;
  private static final int DIAL_TICKS = 20;
  private static final double LINE_HISS = 0.12;

  private long ticks;
  private LineStatus playing;
  private LineHiss hiss;

  static Tone toneAt(LineStatus status, long ticks) {
    if (status == LineStatus.RINGING_OUT && RingCadence.strokeStarting(ticks, false) > 0) {
      return Tone.RINGBACK;
    }
    if (status == LineStatus.DIAL_TONE && ticks % DIAL_TICKS == 0) {
      return Tone.DIAL;
    }
    return status == LineStatus.BUSY && ticks % BUSY_TICKS == 0 ? Tone.BUSY : Tone.NONE;
  }

  static double hissLoudness(int noisePercent) {
    return LINE_HISS * noisePercent / 100.0;
  }

  void tick(MinecraftClient client, LineStatus status) {
    if (status != playing) {
      playing = status;
      ticks = 0;
    }
    if (status == LineStatus.CONNECTED
        && (hiss == null || !client.getSoundManager().isPlaying(hiss))) {
      hiss =
          new LineHiss(
              () -> hissLoudness(ClientSettingsStore.radio().radioNoisePercent()),
              () -> playing == LineStatus.CONNECTED);
      client.getSoundManager().play(hiss);
    }
    switch (toneAt(status, ticks)) {
      case DIAL -> play(client, ModSounds.TELEPHONE_DIAL_TONE);
      case RINGBACK -> play(client, ModSounds.TELEPHONE_RINGBACK);
      case BUSY -> play(client, ModSounds.TELEPHONE_BUSY);
      case NONE -> {}
    }
    ticks++;
  }

  void stop(MinecraftClient client) {
    playing = null;
    if (hiss != null) {
      client.getSoundManager().stop(hiss);
      hiss = null;
    }
  }

  static void play(MinecraftClient client, SoundEvent sound) {
    client.getSoundManager().play(PositionedSoundInstance.master(sound, 1F));
  }
}
