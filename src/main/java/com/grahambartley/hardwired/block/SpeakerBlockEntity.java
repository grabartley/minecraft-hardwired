package com.grahambartley.hardwired.block;

import com.grahambartley.hardwired.config.RadioSettings;
import com.grahambartley.hardwired.registry.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public final class SpeakerBlockEntity extends SoundingBlockEntity {

  public SpeakerBlockEntity(BlockPos pos, BlockState state) {
    super(ModBlockEntities.SPEAKER, pos, state);
  }

  public void repeat(RadioBlockEntity radio) {
    hear(radio.song(), radio.songStart(), radio.strength());
    setVolume(radio.volume());
  }

  @Override
  public int hearing(RadioSettings settings) {
    return settings.speakerHearing();
  }
}
