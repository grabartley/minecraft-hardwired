package com.grahambartley.hardwired.block;

import com.grahambartley.hardwired.device.DialDevice;
import com.grahambartley.hardwired.power.GridState;
import com.grahambartley.hardwired.power.PowerSource;
import com.grahambartley.hardwired.power.SourceKind;
import com.grahambartley.hardwired.registry.ModSounds;
import com.grahambartley.hardwired.wire.WireEngine;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;

public class SourceBlockEntity extends BlockEntity implements DialDevice {

  private static final String CLOSED = "switch_closed";
  private static final String RUNNING = "running";
  private static final String WORKLOAD = "workload";
  private static final String LOAD = "load";
  private static final String SUPPLY = "supply";
  private static final String GRID = "grid";
  private static final float MAX_WORKLOAD = 15F;
  private static final float IDLE_SPEED = 0.35F;
  private static final float EXTRA_SPEED = 1.65F;

  private boolean closed = true;
  private boolean running;
  private int workload;
  private int load;
  private int supply;
  private GridState grid = GridState.NO_SOURCE;

  public SourceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
    super(type, pos, state);
  }

  public SourceKind kind() {
    return getCachedState().getBlock() instanceof PowerSource source
        ? source.sourceKind()
        : SourceKind.CELL;
  }

  public boolean isClosed() {
    return closed;
  }

  public boolean isRunning() {
    return running;
  }

  public int workload() {
    return workload;
  }

  public int load() {
    return load;
  }

  public int supply() {
    return supply;
  }

  public GridState grid() {
    return grid;
  }

  public void toggleSwitch(ServerWorld world) {
    closed = !closed;
    sync();
    WireEngine.of(world).power().changed(pos);
  }

  public void showGrid(
      boolean nowRunning, int nextWorkload, int nextLoad, int nextSupply, GridState nextGrid) {
    boolean startedOrStopped = running != nowRunning;
    boolean changed =
        startedOrStopped
            || workload != nextWorkload
            || load != nextLoad
            || supply != nextSupply
            || grid != nextGrid;
    running = nowRunning;
    workload = nextWorkload;
    load = nextLoad;
    supply = nextSupply;
    grid = nextGrid;
    if (startedOrStopped) {
      play(running ? ModSounds.SOURCE_UP : ModSounds.SOURCE_DOWN, kind().soundVolume());
    }
    if (changed) {
      sync();
    }
  }

  private void play(SoundEvent sound, float volume) {
    if (world != null) {
      world.playSound(null, pos, sound, SoundCategory.BLOCKS, volume, 1F);
    }
  }

  protected void sync() {
    markDirty();
    if (world != null && !world.isClient) {
      world.updateListeners(pos, getCachedState(), getCachedState(), 0);
    }
  }

  @Override
  public float dial(String value, float tickDelta) {
    return switch (value) {
      case "switch" -> closed ? 1F : 0F;
      case "load" -> workload / MAX_WORKLOAD;
      case "sun" -> world == null ? 0.5F : 1F - sunPhase(tickDelta);
      default -> 0F;
    };
  }

  private float sunPhase(float tickDelta) {
    float angle = world.getSkyAngle(tickDelta);
    float climbed = angle > 0.75F ? angle - 0.75F : angle + 0.25F;
    return Math.clamp(climbed * 2F, 0F, 1F);
  }

  @Override
  public boolean isMoving() {
    return running;
  }

  @Override
  public float clipSpeed() {
    return running ? IDLE_SPEED + EXTRA_SPEED * workload / MAX_WORKLOAD : 0F;
  }

  @Override
  protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
    super.writeNbt(nbt, registries);
    writeShared(nbt);
  }

  private void writeShared(NbtCompound nbt) {
    nbt.putBoolean(CLOSED, closed);
    nbt.putBoolean(RUNNING, running);
    nbt.putByte(WORKLOAD, (byte) workload);
    nbt.putInt(LOAD, load);
    nbt.putInt(SUPPLY, supply);
    nbt.putByte(GRID, (byte) grid.ordinal());
  }

  @Override
  protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
    super.readNbt(nbt, registries);
    closed = !nbt.contains(CLOSED) || nbt.getBoolean(CLOSED);
    running = nbt.getBoolean(RUNNING);
    workload = nbt.getByte(WORKLOAD);
    load = nbt.getInt(LOAD);
    supply = nbt.getInt(SUPPLY);
    grid = GridState.byIndex(nbt.getByte(GRID));
  }

  @Override
  public void removeFromCopiedStackNbt(NbtCompound nbt) {
    nbt.remove(CLOSED);
    nbt.remove(RUNNING);
    nbt.remove(WORKLOAD);
    nbt.remove(LOAD);
    nbt.remove(SUPPLY);
    nbt.remove(GRID);
  }

  @Override
  public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
    NbtCompound nbt = new NbtCompound();
    writeShared(nbt);
    return nbt;
  }

  @Override
  public Packet<ClientPlayPacketListener> toUpdatePacket() {
    return BlockEntityUpdateS2CPacket.create(this);
  }
}
