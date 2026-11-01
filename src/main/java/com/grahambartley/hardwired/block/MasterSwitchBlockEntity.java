package com.grahambartley.hardwired.block;

import com.grahambartley.hardwired.device.DialDevice;
import com.grahambartley.hardwired.power.StartOrder;
import com.grahambartley.hardwired.registry.ModBlockEntities;
import com.grahambartley.hardwired.registry.ModSounds;
import com.grahambartley.hardwired.wire.WireEngine;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public final class MasterSwitchBlockEntity extends BlockEntity implements DialDevice {

  private static final String LOAD = "grid_load";
  private static final String SUPPLY = "grid_supply";
  private static final String THROWN_AT = "thrown_at";
  private static final float THROW_TICKS = StartOrder.START_TICKS;
  private static final int SPARK_COUNT = 4;
  private static final double SPARK_SPREAD = 0.08D;
  private static final double SPARK_SPEED = 0.15D;
  private static final double SPARK_OUT = 0.32D;
  private static final double SPARK_UP = 1D;

  private boolean queued;
  private int load;
  private int supply;
  private long thrownAt = Long.MIN_VALUE / 2;

  public MasterSwitchBlockEntity(BlockPos pos, BlockState state) {
    super(ModBlockEntities.MASTER_SWITCH, pos, state);
  }

  public static void tick(
      World world, BlockPos pos, BlockState state, MasterSwitchBlockEntity master) {
    if (!(world instanceof ServerWorld server) || !master.queued) {
      return;
    }
    if (!WireEngine.of(server).power().isStarting(pos)) {
      master.queued = false;
      master.flip(server, state);
    }
  }

  public void requestThrow(ServerWorld server, BlockState state) {
    if (queued) {
      return;
    }
    if (WireEngine.of(server).power().isStarting(pos)) {
      queued = true;
    } else {
      flip(server, state);
    }
  }

  private void flip(ServerWorld server, BlockState state) {
    boolean closing = !state.get(MasterSwitchBlock.CLOSED);
    thrownAt = server.getTime();
    MasterSwitchBlock.setClosed(server, pos, state, closing);
    syncThrow();
    server.playSound(
        null,
        pos,
        closing ? ModSounds.MASTER_PULL : ModSounds.MASTER_OPEN,
        SoundCategory.BLOCKS,
        1.2F,
        1F);
    if (closing) {
      sparks(server, state.get(MasterSwitchBlock.FACING));
      WireEngine.of(server).power().startFrom(pos);
    } else {
      WireEngine.of(server).power().changed(pos);
    }
  }

  private void sparks(ServerWorld server, Direction facing) {
    Vec3d blades =
        Vec3d.ofCenter(pos)
            .add(0, SPARK_UP, 0)
            .add(Vec3d.of(facing.getVector()).multiply(SPARK_OUT));
    server.spawnParticles(
        ParticleTypes.ELECTRIC_SPARK,
        blades.x,
        blades.y,
        blades.z,
        SPARK_COUNT,
        SPARK_SPREAD,
        SPARK_SPREAD,
        SPARK_SPREAD,
        SPARK_SPEED);
  }

  public void showGrid(int nextLoad, int nextSupply) {
    if (load != nextLoad || supply != nextSupply) {
      load = nextLoad;
      supply = nextSupply;
      markDirty();
      if (world != null && !world.isClient) {
        world.updateListeners(pos, getCachedState(), getCachedState(), 0);
      }
    }
  }

  private void syncThrow() {
    markDirty();
    if (world != null) {
      world.updateListeners(pos, getCachedState(), getCachedState(), 0);
    }
  }

  public int load() {
    return load;
  }

  public int supply() {
    return supply;
  }

  @Override
  public float dial(String value, float tickDelta) {
    boolean closed = getCachedState().get(MasterSwitchBlock.CLOSED);
    float time = world == null ? THROW_TICKS : world.getTime() - thrownAt + tickDelta;
    float travelled = Math.clamp(time / THROW_TICKS, 0F, 1F);
    return closed ? travelled : 1F - travelled;
  }

  @Override
  public boolean isMoving() {
    return false;
  }

  @Override
  public float clipSpeed() {
    return 0F;
  }

  @Override
  protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
    super.writeNbt(nbt, registries);
    nbt.putInt(LOAD, load);
    nbt.putInt(SUPPLY, supply);
    nbt.putLong(THROWN_AT, thrownAt);
  }

  @Override
  protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
    super.readNbt(nbt, registries);
    load = nbt.getInt(LOAD);
    supply = nbt.getInt(SUPPLY);
    thrownAt = nbt.getLong(THROWN_AT);
  }

  @Override
  public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
    return createNbt(registries);
  }

  @Override
  public Packet<ClientPlayPacketListener> toUpdatePacket() {
    return BlockEntityUpdateS2CPacket.create(this);
  }
}
