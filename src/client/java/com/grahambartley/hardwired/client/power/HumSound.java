package com.grahambartley.hardwired.client.power;

import com.grahambartley.hardwired.registry.ModSounds;
import java.util.function.DoubleSupplier;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.random.Random;

final class HumSound extends MovingSoundInstance {

  private static final float RISE = 0.01F;
  private static final float FALL = 0.08F;

  private final DoubleSupplier level;

  HumSound(DoubleSupplier level, Random random) {
    super(ModSounds.ELECTRIC_HUM, SoundCategory.AMBIENT, random);
    this.level = level;
    this.repeat = true;
    this.repeatDelay = 0;
    this.relative = true;
    this.attenuationType = SoundInstance.AttenuationType.NONE;
    this.volume = 0F;
  }

  @Override
  public boolean shouldAlwaysPlay() {
    return true;
  }

  @Override
  public void tick() {
    float target = (float) level.getAsDouble();
    volume += Math.clamp(target - volume, -FALL, RISE);
    if (target <= 0F && volume <= 0F) {
      setDone();
    }
  }
}
