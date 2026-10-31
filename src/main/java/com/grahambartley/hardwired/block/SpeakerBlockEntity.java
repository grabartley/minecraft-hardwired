package com.grahambartley.hardwired.block;

import com.grahambartley.hardwired.registry.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.jukebox.JukeboxManager;
import net.minecraft.block.jukebox.JukeboxSong;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class SpeakerBlockEntity extends ElectronicBlockEntity {

  private final JukeboxManager horn = new JukeboxManager(this::markDirty, getPos());

  public SpeakerBlockEntity(BlockPos pos, BlockState state) {
    super(ModBlockEntities.SPEAKER, pos, state);
  }

  public static void tick(World world, BlockPos pos, BlockState state, SpeakerBlockEntity speaker) {
    speaker.horn.tick(world, state);
  }

  public boolean isSounding() {
    return horn.isPlaying();
  }

  public void play(RegistryEntry<JukeboxSong> song) {
    if (horn.getSong() != song.value()) {
      horn.stopPlaying(world, getCachedState());
      horn.startPlaying(world, song);
    }
  }

  public void silence() {
    horn.stopPlaying(world, getCachedState());
  }
}
