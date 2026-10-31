package com.grahambartley.hardwired.block;

import com.grahambartley.hardwired.device.DeviceStatus;
import com.grahambartley.hardwired.device.ElectronicsDriver;
import com.grahambartley.hardwired.device.RadioRun;
import com.grahambartley.hardwired.registry.ModBlockEntities;
import java.util.Optional;
import net.minecraft.block.BlockState;
import net.minecraft.block.jukebox.JukeboxSong;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;

public final class RadioBlockEntity extends ElectronicBlockEntity {

  private static final String DISC = "disc";
  private static final String PLAYED_TICKS = "played_ticks";

  private ItemStack disc = ItemStack.EMPTY;
  private int playedTicks;

  public RadioBlockEntity(BlockPos pos, BlockState state) {
    super(ModBlockEntities.RADIO, pos, state);
  }

  public boolean hasDisc() {
    return !disc.isEmpty();
  }

  public ItemStack disc() {
    return disc;
  }

  public void putDisc(ItemStack stack) {
    disc = stack;
    playedTicks = 0;
    markDirty();
  }

  public ItemStack takeDisc() {
    ItemStack taken = disc;
    disc = ItemStack.EMPTY;
    playedTicks = 0;
    markDirty();
    return taken;
  }

  public Optional<RegistryEntry<JukeboxSong>> song() {
    return world == null
        ? Optional.empty()
        : JukeboxSong.getSongEntryFromStack(world.getRegistryManager(), disc);
  }

  public boolean songEnded() {
    return song()
        .map(entry -> RadioRun.songEnded(playedTicks, entry.value().getLengthInTicks()))
        .orElse(false);
  }

  public void advance(DeviceStatus status) {
    int next = RadioRun.playedAfter(playedTicks, status, ElectronicsDriver.BEAT_TICKS);
    if (next != playedTicks) {
      playedTicks = next;
      markDirty();
    }
  }

  @Override
  protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
    super.writeNbt(nbt, registries);
    if (hasDisc()) {
      nbt.put(DISC, disc.encode(registries));
    }
    nbt.putInt(PLAYED_TICKS, playedTicks);
  }

  @Override
  protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
    super.readNbt(nbt, registries);
    disc =
        nbt.contains(DISC, NbtElement.COMPOUND_TYPE)
            ? ItemStack.fromNbt(registries, nbt.getCompound(DISC)).orElse(ItemStack.EMPTY)
            : ItemStack.EMPTY;
    playedTicks = nbt.getInt(PLAYED_TICKS);
  }

  @Override
  public void removeFromCopiedStackNbt(NbtCompound nbt) {
    super.removeFromCopiedStackNbt(nbt);
    nbt.remove(DISC);
    nbt.remove(PLAYED_TICKS);
  }
}
