package com.grahambartley.hardwired.wire;

import com.grahambartley.hardwired.device.WireDevice;
import com.grahambartley.hardwired.power.PowerGrids;
import com.grahambartley.hardwired.registry.ModItems;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;

public final class WireEngine {

  private final ServerWorld world;
  private final WireState state;
  private final WireGrid grid;
  private final PowerGrids power;
  private final LongOpenHashSet pendingPushes = new LongOpenHashSet();
  private final LongOpenHashSet pendingSurfaceChecks = new LongOpenHashSet();

  public WireEngine(ServerWorld world) {
    this.world = world;
    this.state = world.getPersistentStateManager().getOrCreate(WireState.TYPE, WireState.KEY);
    this.grid = state.grid();
    this.power = new PowerGrids(world, this, grid);
  }

  public static WireEngine of(ServerWorld world) {
    return ((WiredWorld) world).hardwired$wires();
  }

  public WireGrid grid() {
    return grid;
  }

  public PowerGrids power() {
    return power;
  }

  public WireNetwork lay(WireCell cell) {
    WireNetwork network = grid.add(cell);
    state.markDirty();
    return network;
  }

  public WireNetwork layJoining(WireCell cell) {
    lay(cell);
    if (cell.isSealed()) {
      grid.openNetworksAround(cell).forEach(open -> reown(open, cell.owner()));
    }
    return grid.networkOf(cell);
  }

  public void reown(WireNetwork network, UUID owner) {
    grid.reown(network, owner).forEach(this::dropItem);
    state.markDirty();
  }

  public void cut(WireCell cell) {
    grid.remove(cell);
    state.markDirty();
  }

  public boolean isLive(BlockPos pos) {
    return grid.isLive(pos.asLong());
  }

  public boolean isOn(BlockPos pos) {
    return grid.isOn(pos.asLong());
  }

  public boolean isOn(BlockPos pos, Hue color) {
    for (WireNetwork network : grid.networksTouching(pos)) {
      if (network.color() == color && network.isOn()) {
        return true;
      }
    }
    return false;
  }

  public boolean isOnFrom(BlockPos pos, Direction side) {
    TouchPoint touch = new TouchPoint(pos, side.getOpposite());
    for (WireNetwork network : grid.networksTouching(pos)) {
      if (network.isOn() && network.touches().contains(touch)) {
        return true;
      }
    }
    return false;
  }

  public LampLook lookAt(BlockPos pos) {
    return lookNetworkAt(pos, network -> true).map(WireNetwork::look).orElse(null);
  }

  public LampLook lookAt(BlockPos pos, WireViewer viewer) {
    return lookNetworkAt(pos, viewer::canSee).map(WireNetwork::look).orElse(null);
  }

  public long lookStampAt(BlockPos pos) {
    return lookNetworkAt(pos, network -> true).map(WireNetwork::lookStamp).orElse(0L);
  }

  private Optional<WireNetwork> lookNetworkAt(BlockPos pos, Predicate<WireNetwork> visible) {
    return grid.networksTouching(pos).stream()
        .filter(visible)
        .filter(network -> network.look() != null)
        .min(Comparator.comparing(WireNetwork::color));
  }

  public long nextLookStamp() {
    state.markDirty();
    return grid.nextLookStamp();
  }

  public void applyLook(BlockPos pos, LampLook look) {
    long stamp = grid.nextLookStamp();
    for (WireNetwork network : grid.networksTouching(pos)) {
      network.setLook(look);
      network.stampLook(stamp);
      pushNetwork(network);
    }
    state.markDirty();
  }

  public <T> Optional<T> findOnNetwork(BlockPos pos, Class<T> type) {
    return allOnNetwork(pos, type).stream().findFirst();
  }

  public <T> List<T> allOnNetwork(BlockPos pos, Class<T> type) {
    return loadedOnNetwork(pos).stream()
        .map(world::getBlockEntity)
        .filter(type::isInstance)
        .map(type::cast)
        .toList();
  }

  public List<BlockPos> loadedOnNetwork(BlockPos pos) {
    return grid.networksTouching(pos).stream()
        .flatMap(network -> network.touches().stream())
        .map(TouchPoint::pos)
        .distinct()
        .filter(this::isLoaded)
        .sorted()
        .toList();
  }

  public boolean isWired(BlockPos pos) {
    return !grid.networksTouching(pos).isEmpty();
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
      power.changed(pos);
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
          power.changed(network);
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
    power.tick();
    long[] touchChanges = grid.drainTouchChanges();
    pendingPushes.addAll(LongList.of(touchChanges));
    for (long touch : touchChanges) {
      power.changed(BlockPos.fromLong(touch));
    }
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
    dropItem(cell);
  }

  private void dropItem(WireCell cell) {
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
      power.changed(network);
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
    LongOpenHashSet read = new LongOpenHashSet();
    LongOpenHashSet driving = new LongOpenHashSet();
    for (TouchPoint touch : new ArrayList<>(network.touches())) {
      if (canRead(touch.pos())) {
        long key = touch.pos().asLong();
        read.add(key);
        if (isInput(touch, network.color())) {
          driving.add(key);
        }
      }
    }
    for (long key : read) {
      changed |=
          driving.contains(key)
              ? network.rememberedInputs().add(key)
              : network.rememberedInputs().remove(key);
    }
    return changed;
  }

  private static boolean hasActiveInput(WireNetwork network) {
    return !network.rememberedInputs().isEmpty();
  }

  public boolean canRead(BlockPos pos) {
    return (isLoaded(pos) && world.getBlockState(pos).getBlock() instanceof WireDevice)
        || VanillaPower.isReadable(world, pos);
  }

  public boolean isInput(TouchPoint touch, Hue color) {
    BlockState blockState = world.getBlockState(touch.pos());
    if (blockState.getBlock() instanceof WireDevice device) {
      return device.isDrivingFrom(
          world, touch.pos(), blockState, color, touch.awayFromWire().getOpposite());
    }
    return VanillaPower.suppliesWire(world, touch);
  }

  public void pushNetwork(WireNetwork network) {
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
