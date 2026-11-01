package com.grahambartley.hardwired.client.radio;

import java.util.function.DoubleSupplier;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;

final class NoiseSoundInstance extends DeviceSoundInstance {

  NoiseSoundInstance(
      SoundEvent noise,
      BlockPos pos,
      int hearing,
      long seed,
      boolean looping,
      DoubleSupplier loudness) {
    super(noise, pos, hearing, seed, false, loudness);
    this.repeat = looping;
  }
}
