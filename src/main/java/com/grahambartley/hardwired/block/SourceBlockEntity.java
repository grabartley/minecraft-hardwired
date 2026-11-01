package com.grahambartley.hardwired.block;

import com.grahambartley.hardwired.device.DialDevice;
import com.grahambartley.hardwired.power.PowerDials;
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

  private boolean closed = true;
  private boolean running;
  private int workload;

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

  public void toggleSwitch(ServerWorld world) {
    closed = !closed;
    sync();
    WireEngine.of(world).power().changed(pos);
  }

  public void showGrid(boolean nowRunning, int nextWorkload) {
    boolean startedOrStopped = running != nowRunning;
    boolean changed = startedOrStopped || workload != nextWorkload;
    running = nowRunning;
    workload = nextWorkload;
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
      case "load" -> PowerDials.gauge(workload);
      case "sun" -> world == null ? 0.5F : 1F - PowerDials.sunPhase(world.getSkyAngle(tickDelta));
      default -> 0F;
    };
  }

  @Override
  public boolean isMoving() {
    return running;
  }

  @Override
  public float clipSpeed() {
    return PowerDials.clipSpeed(running, workload);
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
  }

  @Override
  protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
    super.readNbt(nbt, registries);
    closed = !nbt.contains(CLOSED) || nbt.getBoolean(CLOSED);
    running = nbt.getBoolean(RUNNING);
    workload = nbt.getByte(WORKLOAD);
  }

  @Override
  public void removeFromCopiedStackNbt(NbtCompound nbt) {
    nbt.remove(CLOSED);
    nbt.remove(RUNNING);
    nbt.remove(WORKLOAD);
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
