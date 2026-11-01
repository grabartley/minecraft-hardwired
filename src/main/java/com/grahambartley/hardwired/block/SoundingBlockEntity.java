package com.grahambartley.hardwired.block;

import com.grahambartley.hardwired.config.RadioSettings;
import com.grahambartley.hardwired.radio.ReceptionMix;
import com.grahambartley.hardwired.radio.SongClock;
import java.util.Objects;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

public abstract class SoundingBlockEntity extends ElectronicBlockEntity {

  private static final String SONG = "song";
  private static final String SONG_START = "song_start";
  private static final String STRENGTH = "strength";
  private static final String VOLUME = "volume";
  private static final double NOTE_HEIGHT = 1.0;

  @Nullable private Identifier song;
  private long songStart = SongClock.STOPPED;
  private float strength;
  private int volume = ReceptionMix.MAX_VOLUME;

  protected SoundingBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
    super(type, pos, state);
  }

  public abstract int hearing(RadioSettings settings);

  @Nullable
  public Identifier song() {
    return song;
  }

  public long songStart() {
    return songStart;
  }

  public double strength() {
    return strength;
  }

  public int volume() {
    return volume;
  }

  public boolean sounding() {
    return song != null;
  }

  public void setVolume(int next) {
    int clamped = Math.clamp(next, 0, ReceptionMix.MAX_VOLUME);
    if (clamped != volume) {
      volume = clamped;
      sync();
    }
  }

  public void hear(@Nullable Identifier nextSong, long nextStart, double nextStrength) {
    float rounded = (float) nextStrength;
    if (!Objects.equals(song, nextSong) || songStart != nextStart || strength != rounded) {
      song = nextSong;
      songStart = nextStart;
      strength = rounded;
      sync();
    }
  }

  public void silence() {
    hear(null, SongClock.STOPPED, 0);
  }

  public void noteParticle(ServerWorld server) {
    if (sounding()) {
      Vec3d top = Vec3d.ofBottomCenter(pos).add(0, NOTE_HEIGHT, 0);
      server.spawnParticles(ParticleTypes.NOTE, top.x, top.y, top.z, 0, 0, 0, 0, 1);
    }
  }

  private void writeSound(NbtCompound nbt) {
    if (song != null) {
      nbt.putString(SONG, song.toString());
    }
    nbt.putLong(SONG_START, songStart);
    nbt.putFloat(STRENGTH, strength);
    nbt.putInt(VOLUME, volume);
  }

  @Override
  protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
    super.writeNbt(nbt, registries);
    writeSound(nbt);
  }

  @Override
  protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
    super.readNbt(nbt, registries);
    song = nbt.contains(SONG) ? Identifier.tryParse(nbt.getString(SONG)) : null;
    songStart = nbt.contains(SONG_START) ? nbt.getLong(SONG_START) : SongClock.STOPPED;
    strength = nbt.getFloat(STRENGTH);
    volume = nbt.contains(VOLUME) ? nbt.getInt(VOLUME) : ReceptionMix.MAX_VOLUME;
  }

  @Override
  public void removeFromCopiedStackNbt(NbtCompound nbt) {
    super.removeFromCopiedStackNbt(nbt);
    nbt.remove(SONG);
    nbt.remove(SONG_START);
    nbt.remove(STRENGTH);
    nbt.remove(VOLUME);
  }

  @Override
  public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
    NbtCompound nbt = super.toInitialChunkDataNbt(registries);
    writeSound(nbt);
    return nbt;
  }
}
