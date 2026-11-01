package com.grahambartley.hardwired.block;

import com.grahambartley.hardwired.config.RadioSettings;
import com.grahambartley.hardwired.registry.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;

public final class SpeakerBlockEntity extends SoundingBlockEntity {

  private static final String DIAL = "dial";

  private int dial;

  public SpeakerBlockEntity(BlockPos pos, BlockState state) {
    super(ModBlockEntities.SPEAKER, pos, state);
  }

  @Override
  public void removeFromCopiedStackNbt(NbtCompound nbt) {
    super.removeFromCopiedStackNbt(nbt);
    nbt.remove(DIAL);
  }

  public void repeat(RadioBlockEntity radio) {
    hear(radio.station(), radio.song(), radio.songStart(), radio.strength());
    setVolume(radio.volume());
    if (dial != radio.frequency()) {
      dial = radio.frequency();
      sync();
    }
  }

  @Override
  public int dial() {
    return dial;
  }

  @Override
  protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
    super.writeNbt(nbt, registries);
    nbt.putInt(DIAL, dial);
  }

  @Override
  protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
    super.readNbt(nbt, registries);
    dial = nbt.getInt(DIAL);
  }

  @Override
  public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
    NbtCompound nbt = super.toInitialChunkDataNbt(registries);
    nbt.putInt(DIAL, dial);
    return nbt;
  }

  @Override
  public int hearing(RadioSettings settings) {
    return settings.speakerHearing();
  }
}
