package com.grahambartley.hardwired.block;

import com.grahambartley.hardwired.config.RadioSettings;
import com.grahambartley.hardwired.radio.Frequency;
import com.grahambartley.hardwired.registry.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class RadioBlockEntity extends SoundingBlockEntity {

  private static final String ON = "on";
  private static final String FREQUENCY = "frequency";
  private static final String OLD_DISC = "disc";
  private static final String HISSING = "hissing";

  private boolean on = true;
  private int frequency = Frequency.DEFAULT;
  private ItemStack oldDisc = ItemStack.EMPTY;
  private boolean hissing;

  public RadioBlockEntity(BlockPos pos, BlockState state) {
    super(ModBlockEntities.RADIO, pos, state);
  }

  public static void spillOldDisc(
      World world, BlockPos pos, BlockState state, RadioBlockEntity radio) {
    if (!radio.oldDisc.isEmpty()) {
      ItemScatterer.spawn(world, pos.getX(), pos.getY(), pos.getZ(), radio.oldDisc);
      radio.oldDisc = ItemStack.EMPTY;
      radio.markDirty();
    }
  }

  public boolean isOn() {
    return on;
  }

  public int frequency() {
    return frequency;
  }

  @Override
  public int dial() {
    return frequency;
  }

  @Override
  public boolean hisses() {
    return hissing;
  }

  public void setHissing(boolean next) {
    if (hissing != next) {
      hissing = next;
      sync();
    }
  }

  public void set(boolean nextOn, int nextFrequency, int nextVolume) {
    int snapped = Frequency.snap(nextFrequency);
    boolean changed = on != nextOn || frequency != snapped || volume() != nextVolume;
    on = nextOn;
    frequency = snapped;
    setVolume(nextVolume);
    if (changed) {
      sync();
    }
  }

  @Override
  public int hearing(RadioSettings settings) {
    return settings.radioHearing();
  }

  private void writeDial(NbtCompound nbt) {
    nbt.putBoolean(ON, on);
    nbt.putInt(FREQUENCY, frequency);
  }

  @Override
  protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
    super.writeNbt(nbt, registries);
    writeDial(nbt);
    if (!oldDisc.isEmpty()) {
      nbt.put(OLD_DISC, oldDisc.encode(registries));
    }
  }

  @Override
  protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
    super.readNbt(nbt, registries);
    on = !nbt.contains(ON) || nbt.getBoolean(ON);
    frequency = nbt.contains(FREQUENCY) ? Frequency.snap(nbt.getInt(FREQUENCY)) : Frequency.DEFAULT;
    hissing = nbt.getBoolean(HISSING);
    if (nbt.contains(OLD_DISC, NbtElement.COMPOUND_TYPE)) {
      oldDisc = ItemStack.fromNbt(registries, nbt.getCompound(OLD_DISC)).orElse(ItemStack.EMPTY);
    }
  }

  @Override
  public void removeFromCopiedStackNbt(NbtCompound nbt) {
    super.removeFromCopiedStackNbt(nbt);
    nbt.remove(ON);
    nbt.remove(FREQUENCY);
    nbt.remove(OLD_DISC);
  }

  @Override
  public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
    NbtCompound nbt = super.toInitialChunkDataNbt(registries);
    writeDial(nbt);
    nbt.putBoolean(HISSING, hissing);
    return nbt;
  }
}
