package com.grahambartley.hardwired.screen;

import com.grahambartley.hardwired.phone.LineStatus;
import com.grahambartley.hardwired.phone.RingCadence;
import com.grahambartley.hardwired.registry.ModSounds;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;

final class LineTones {

  enum Tone {
    NONE,
    RINGBACK,
    BUSY
  }

  private static final int BUSY_TICKS = 10;

  private long ticks;
  private LineStatus playing;

  static Tone toneAt(LineStatus status, long ticks) {
    if (status == LineStatus.RINGING_OUT && RingCadence.strokeStarting(ticks, false) > 0) {
      return Tone.RINGBACK;
    }
    return status == LineStatus.BUSY && ticks % BUSY_TICKS == 0 ? Tone.BUSY : Tone.NONE;
  }

  void tick(MinecraftClient client, LineStatus status) {
    if (status != playing) {
      playing = status;
      ticks = 0;
    }
    switch (toneAt(status, ticks)) {
      case RINGBACK -> play(client, ModSounds.TELEPHONE_RINGBACK);
      case BUSY -> play(client, ModSounds.TELEPHONE_BUSY);
      case NONE -> {}
    }
    ticks++;
  }

  static void play(MinecraftClient client, SoundEvent sound) {
    client.getSoundManager().play(PositionedSoundInstance.master(sound, 1F));
  }
}
