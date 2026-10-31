package com.grahambartley.hardwired.block;

import com.grahambartley.hardwired.Hardwired;
import com.grahambartley.hardwired.registry.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.jukebox.JukeboxManager;
import net.minecraft.block.jukebox.JukeboxSong;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class SpeakerBlockEntity extends ElectronicBlockEntity {

  private static final String SONG = "song";
  private static final String SONG_TICKS = "song_ticks";

  private final JukeboxManager horn = new JukeboxManager(this::markDirty, getPos());
  private RegistryEntry<JukeboxSong> song;

  public SpeakerBlockEntity(BlockPos pos, BlockState state) {
    super(ModBlockEntities.SPEAKER, pos, state);
  }

  public static void tick(World world, BlockPos pos, BlockState state, SpeakerBlockEntity speaker) {
    speaker.horn.tick(world, state);
  }

  public boolean isSounding() {
    return horn.isPlaying();
  }

  public void play(RegistryEntry<JukeboxSong> next) {
    if (horn.getSong() != next.value()) {
      horn.stopPlaying(world, getCachedState());
      horn.startPlaying(world, next);
      song = next;
    }
    markDirty();
  }

  public void silence() {
    horn.stopPlaying(world, getCachedState());
  }

  @Override
  protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
    super.writeNbt(nbt, registries);
    if (horn.isPlaying()) {
      nbt.put(
          SONG,
          JukeboxSong.ENTRY_CODEC
              .encodeStart(registries.getOps(NbtOps.INSTANCE), song)
              .getOrThrow());
      nbt.putLong(SONG_TICKS, horn.getTicksSinceSongStarted());
    }
  }

  @Override
  protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
    super.readNbt(nbt, registries);
    if (nbt.contains(SONG)) {
      JukeboxSong.ENTRY_CODEC
          .parse(registries.getOps(NbtOps.INSTANCE), nbt.get(SONG))
          .resultOrPartial(Hardwired.LOGGER::error)
          .ifPresent(
              saved -> {
                song = saved;
                horn.setValues(saved, nbt.getLong(SONG_TICKS));
              });
    }
  }

  @Override
  public void removeFromCopiedStackNbt(NbtCompound nbt) {
    super.removeFromCopiedStackNbt(nbt);
    nbt.remove(SONG);
    nbt.remove(SONG_TICKS);
  }
}
