package com.grahambartley.hardwired.screen;

import com.grahambartley.hardwired.registry.ModSounds;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;

final class LineHiss extends MovingSoundInstance {

  private final DoubleSupplier loudness;
  private final BooleanSupplier live;

  LineHiss(DoubleSupplier loudness, BooleanSupplier live) {
    super(ModSounds.RADIO_HISS, SoundCategory.BLOCKS, SoundInstance.createRandom());
    this.loudness = loudness;
    this.live = live;
    repeat = true;
    relative = true;
    attenuationType = AttenuationType.NONE;
    volume = (float) loudness.getAsDouble();
  }

  @Override
  public void tick() {
    if (!live.getAsBoolean()) {
      setDone();
      return;
    }
    volume = (float) loudness.getAsDouble();
  }
}
