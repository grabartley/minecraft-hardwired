package com.grahambartley.hardwired.wire;

import com.grahambartley.hardwired.device.WireDevice;
import com.grahambartley.hardwired.registry.ModItems;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

public final class WireEngine {

  private final ServerWorld world;
  private final WireState state;
  private final WireGrid grid;
  private final LongOpenHashSet pendingPushes = new LongOpenHashSet();
  private final LongOpenHashSet pendingSurfaceChecks = new LongOpenHashSet();

  public WireEngine(ServerWorld world) {
    this.world = world;
    this.state = world.getPersistentStateManager().getOrCreate(WireState.TYPE, WireState.KEY);
    this.grid = state.grid();
  }

  public static WireEngine of(ServerWorld world) {
    return ((WiredWorld) world).hardwired$wires();
  }

  public WireGrid grid() {
    return grid;
  }

  public WireNetwork lay(WireCell cell) {
    WireNetwork network = grid.add(cell);
    state.markDirty();
    return network;
  }

  public void cut(WireCell cell) {
    grid.remove(cell);
    state.markDirty();
  }

  public boolean isPowered(BlockPos pos) {
    return grid.isPowered(pos.asLong());
  }

  public LampLook lookAt(BlockPos pos) {
    return grid.networksTouching(pos).stream()
        .filter(network -> network.look() != null)
        .min(Comparator.comparing(WireNetwork::color))
        .map(WireNetwork::look)
        .orElse(null);
  }

  public void applyLook(BlockPos pos, LampLook look) {
    for (WireNetwork network : grid.networksTouching(pos)) {
      network.setLook(look);
      pushNetwork(network);
    }
    state.markDirty();
  }

  public <T> Optional<T> findOnNetwork(BlockPos pos, Class<T> type) {
    return grid.networksTouching(pos).stream()
        .flatMap(network -> network.touches().stream())
        .map(TouchPoint::pos)
        .filter(this::isLoaded)
        .sorted()
        .map(world::getBlockEntity)
        .filter(type::isInstance)
        .map(type::cast)
        .findFirst();
  }

  public void refresh(BlockPos pos) {
    for (WireNetwork network : grid.networksTouching(pos)) {
      grid.markDirty(network);
    }
  }

  public void onBlockChanged(BlockPos pos, BlockState oldState, BlockState newState) {
    if (!grid.networksTouching(pos).isEmpty()) {
      refresh(pos);
      pendingPushes.add(pos.asLong());
      grid.networksTouching(pos).forEach(grid::markChanged);
    }
    if (!oldState.isOf(newState.getBlock()) && !grid.cellsOn(pos).isEmpty()) {
      pendingSurfaceChecks.add(pos.asLong());
    }
  }

  public void onChunkLoaded(ChunkPos chunk) {
    if (!hasWireNear(chunk)) {
      return;
    }
    for (WireNetwork network : grid.networks()) {
      if (network.touches().stream()
          .anyMatch(touch -> VanillaPower.readsFrom(touch.pos(), chunk))) {
        grid.markDirty(network);
      }
      for (TouchPoint touch : network.touches()) {
        if (isIn(touch, chunk)) {
          pendingPushes.add(touch.pos().asLong());
        }
      }
    }
  }

  private boolean hasWireNear(ChunkPos chunk) {
    int radius = (VanillaPower.READ_REACH >> 4) + 2;
    for (int x = chunk.x - radius; x <= chunk.x + radius; x++) {
      for (int z = chunk.z - radius; z <= chunk.z + radius; z++) {
        if (!grid.cellsInChunk(ChunkPos.toLong(x, z)).isEmpty()) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean isIn(TouchPoint touch, ChunkPos chunk) {
    return ChunkPos.toLong(touch.pos()) == chunk.toLong();
  }

  public void tick() {
    dropUnsupportedWire();
    for (WireNetwork network : grid.drainDirty()) {
      evaluate(network);
    }
    pendingPushes.addAll(LongList.of(grid.drainTouchChanges()));
    if (!pendingPushes.isEmpty()) {
      long[] pushes = pendingPushes.toLongArray();
      pendingPushes.clear();
      for (long pos : pushes) {
        push(BlockPos.fromLong(pos));
      }
    }
  }

  private void dropUnsupportedWire() {
    if (pendingSurfaceChecks.isEmpty()) {
      return;
    }
    long[] checks = pendingSurfaceChecks.toLongArray();
    pendingSurfaceChecks.clear();
    for (long packed : checks) {
      BlockPos pos = BlockPos.fromLong(packed);
      for (WireCell cell : List.copyOf(grid.cellsOn(pos))) {
        if (!WireRules.accepts(world, pos, cell.face().side())) {
          drop(cell);
        }
      }
    }
  }

  public void dropWireOn(BlockPos pos) {
    List.copyOf(grid.cellsOn(pos)).forEach(this::drop);
  }

  private void drop(WireCell cell) {
    cut(cell);
    Block.dropStack(world, cell.face().pos(), new ItemStack(ModItems.wire(cell.color())));
  }

  private void evaluate(WireNetwork network) {
    releaseOrphanedLook(network);
    boolean inputsChanged = readInputs(network);
    boolean active = hasActiveInput(network) && !network.isTripped();
    if (active != network.isOn() && network.tripDetector().recordChange(world.getTime())) {
      network.setTripped(true);
      active = false;
    }
    if (active != network.isOn()) {
      network.setOn(active);
      pushNetwork(network);
    } else if (inputsChanged) {
      grid.markChanged(network);
    }
    state.markDirty();
  }

  private void releaseOrphanedLook(WireNetwork network) {
    if (network.look() == null) {
      return;
    }
    for (TouchPoint touch : network.touches()) {
      if (!isLoaded(touch.pos())
          || (world.getBlockState(touch.pos()).getBlock() instanceof WireDevice device
              && device.holdsLook())) {
        return;
      }
    }
    network.setLook(null);
    pushNetwork(network);
  }

  private boolean readInputs(WireNetwork network) {
    boolean changed = network.rememberedInputs().retainAll(network.touchedPositions.keySet());
    for (TouchPoint touch : new ArrayList<>(network.touches())) {
      if (VanillaPower.isReadable(world, touch.pos())) {
        long key = touch.pos().asLong();
        changed |=
            isInput(touch, network.color())
                ? network.rememberedInputs().add(key)
                : network.rememberedInputs().remove(key);
      }
    }
    return changed;
  }

  private static boolean hasActiveInput(WireNetwork network) {
    return !network.rememberedInputs().isEmpty();
  }

  public boolean isInput(TouchPoint touch, Hue color) {
    BlockState blockState = world.getBlockState(touch.pos());
    if (blockState.getBlock() instanceof WireDevice device) {
      return device.isDriving(world, touch.pos(), blockState, color);
    }
    return VanillaPower.suppliesWire(world, touch);
  }

  private void pushNetwork(WireNetwork network) {
    for (TouchPoint touch : network.touches()) {
      pendingPushes.add(touch.pos().asLong());
    }
    grid.markChanged(network);
  }

  private void push(BlockPos pos) {
    if (!isLoaded(pos)) {
      return;
    }
    BlockState blockState = world.getBlockState(pos);
    if (blockState.getBlock() instanceof WireDevice device) {
      device.onWireChange(world, pos, blockState);
    } else if (!blockState.isAir()) {
      world.updateNeighbor(pos, Blocks.AIR, pos);
    }
  }

  public boolean isLoaded(BlockPos pos) {
    return world.getChunkManager().isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4);
  }
}
