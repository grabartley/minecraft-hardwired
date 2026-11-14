package com.grahambartley.hardwired.client.radio;

import com.grahambartley.hardwired.radio.RadioTone;
import com.grahambartley.hardwired.radio.VoiceJitter;
import com.grahambartley.hardwired.registry.ModSounds;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.sound.AudioStream;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundLoader;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;

final class CallVoiceSoundInstance extends MovingSoundInstance {

  private final VoiceJitter jitter;

  CallVoiceSoundInstance(VoiceJitter jitter, long seed) {
    super(ModSounds.TELEPHONE_VOICE, SoundCategory.VOICE, Random.create(seed));
    this.jitter = jitter;
    this.relative = true;
    this.attenuationType = SoundInstance.AttenuationType.NONE;
  }

  @Override
  public void tick() {}

  void silence() {
    setDone();
  }

  @Override
  public CompletableFuture<AudioStream> getAudioStream(
      SoundLoader loader, Identifier id, boolean repeatInstantly) {
    return CompletableFuture.completedFuture(
        new RadioToneStream(new VoiceStream(jitter), RadioTone.Band.TELEPHONE));
  }

  @Override
  public boolean shouldAlwaysPlay() {
    return true;
  }
}
