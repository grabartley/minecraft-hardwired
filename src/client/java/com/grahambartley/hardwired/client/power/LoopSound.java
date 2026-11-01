package com.grahambartley.hardwired.client.power;

import com.grahambartley.hardwired.block.SourceBlockEntity;
import com.grahambartley.hardwired.power.PowerSoundLevels;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

final class LoopSound extends MovingSoundInstance {

  private static final float FADE = 0.06F;

  private final SourceBlockEntity source;
  private boolean fading;

  LoopSound(SoundEvent event, SourceBlockEntity source, Random random) {
    super(event, SoundCategory.BLOCKS, random);
    this.source = source;
    BlockPos pos = source.getPos();
    this.x = pos.getX() + 0.5;
    this.y = pos.getY() + 0.5;
    this.z = pos.getZ() + 0.5;
    this.repeat = true;
    this.repeatDelay = 0;
    this.volume = 0F;
    this.pitch = PowerSoundLevels.loopPitch(source.workload());
  }

  void fadeOut() {
    fading = true;
  }

  @Override
  public boolean shouldAlwaysPlay() {
    return true;
  }

  @Override
  public void tick() {
    boolean running = !fading && source.isRunning() && !source.isRemoved();
    float targetVolume = running ? PowerSoundLevels.loopVolume(source.workload()) : 0F;
    float targetPitch = PowerSoundLevels.loopPitch(running ? source.workload() : 0);
    volume += Math.clamp(targetVolume - volume, -FADE, FADE);
    pitch += (targetPitch - pitch) * FADE * 2F;
    if (!running && volume <= 0F) {
      setDone();
    }
  }
}
