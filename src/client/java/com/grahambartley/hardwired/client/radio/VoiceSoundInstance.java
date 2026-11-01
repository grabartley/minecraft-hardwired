package com.grahambartley.hardwired.client.radio;

import com.grahambartley.hardwired.radio.VoiceJitter;
import com.grahambartley.hardwired.registry.ModSounds;
import java.util.concurrent.CompletableFuture;
import java.util.function.DoubleSupplier;
import net.minecraft.client.sound.AudioStream;
import net.minecraft.client.sound.SoundLoader;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

final class VoiceSoundInstance extends DeviceSoundInstance {

  private final VoiceJitter jitter;

  VoiceSoundInstance(
      VoiceJitter jitter, BlockPos pos, int hearing, long seed, DoubleSupplier loudness) {
    super(ModSounds.RADIO_VOICE, pos, hearing, seed, true, loudness);
    this.jitter = jitter;
  }

  @Override
  public CompletableFuture<AudioStream> getAudioStream(
      SoundLoader loader, Identifier id, boolean repeatInstantly) {
    return CompletableFuture.completedFuture(new VoiceStream(jitter));
  }
}
