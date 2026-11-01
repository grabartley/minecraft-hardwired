package com.grahambartley.hardwired.client.radio;

import java.util.function.DoubleSupplier;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.client.sound.Sound;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.sound.WeightedSoundSet;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

abstract class DeviceSoundInstance extends MovingSoundInstance {

  private final DoubleSupplier loudness;
  private final int hearing;
  private final boolean streamed;

  DeviceSoundInstance(
      SoundEvent event,
      BlockPos pos,
      int hearing,
      long seed,
      boolean streamed,
      DoubleSupplier loudness) {
    super(event, SoundCategory.RECORDS, Random.create(seed));
    this.loudness = loudness;
    this.hearing = hearing;
    this.streamed = streamed;
    this.x = pos.getX() + 0.5;
    this.y = pos.getY() + 0.5;
    this.z = pos.getZ() + 0.5;
    this.volume = (float) loudness.getAsDouble();
  }

  @Override
  public void tick() {
    volume = (float) loudness.getAsDouble();
  }

  public void silence() {
    setDone();
  }

  @Override
  public WeightedSoundSet getSoundSet(SoundManager manager) {
    WeightedSoundSet set = super.getSoundSet(manager);
    if (sound != null
        && sound != SoundManager.MISSING_SOUND
        && sound != SoundManager.INTENTIONALLY_EMPTY_SOUND) {
      sound =
          new Sound(
              sound.getIdentifier(),
              sound.getVolume(),
              sound.getPitch(),
              sound.getWeight(),
              Sound.RegistrationType.FILE,
              streamed,
              false,
              hearing);
    }
    return set;
  }

  @Override
  public boolean shouldAlwaysPlay() {
    return true;
  }
}
