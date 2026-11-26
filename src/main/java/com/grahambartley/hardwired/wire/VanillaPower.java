package com.grahambartley.hardwired.wire;

import com.grahambartley.hardwired.device.WireDevice;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.block.AbstractRedstoneGateBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;

public final class VanillaPower {

  private static final int DUST_REACH = 15;
  public static final int READ_REACH = DUST_REACH + 3;
  private static final int UPDATE_REACH = 2;
  private static final Direction[] DIRECTIONS = Direction.values();

  private static boolean ignoringWire;
  private static BlockPos quasiGuarded;

  private VanillaPower() {}

  public static boolean drives(ServerWorld world, BlockPos emitter, Direction direction) {
    if (ignoringWire) {
      return false;
    }
    WireGrid grid = ((WiredWorld) world).hardwired$wires().grid();
    if (!grid.isTouched()) {
      return false;
    }
    BlockPos receiver = emitter.offset(direction.getOpposite());
    if (quasiGuarded != null && !quasiGuarded.equals(receiver)) {
      return false;
    }
    return grid.isLive(receiver.asLong());
  }

  public static void guardQuasiConnectivity(BlockPos pos) {
    quasiGuarded = pos;
  }

  public static void clearQuasiGuard() {
    quasiGuarded = null;
  }

  public static boolean isComponent(BlockState state) {
    return state.isIn(WireRules.OUTPUTS);
  }

  public static List<BlockPos> cells(
      BlockPos pos, BlockState state, Function<BlockPos, BlockState> states) {
    if (state.isIn(BlockTags.DOORS) && DoorCells.spansMoreThanTwoHalves(state)) {
      return DoorCells.of(pos, state, states);
    }
    return state.contains(Properties.DOUBLE_BLOCK_HALF)
        ? WireDevice.twoTall(pos, state.get(Properties.DOUBLE_BLOCK_HALF))
        : List.of(pos);
  }

  public static boolean suppliesWire(ServerWorld world, TouchPoint touch) {
    BlockState state = world.getBlockState(touch.pos());
    if (state.isAir()) {
      return false;
    }
    ignoringWire = true;
    try {
      if (state.isOf(Blocks.REDSTONE_WIRE)) {
        return dustIsFed(world, touch.pos());
      }
      if (state.emitsRedstonePower()) {
        return emits(world, touch, state);
      }
      return receives(world, touch.pos());
    } finally {
      ignoringWire = false;
    }
  }

  private static boolean emits(ServerWorld world, TouchPoint touch, BlockState state) {
    if (state.getBlock() instanceof AbstractRedstoneGateBlock) {
      return state.getWeakRedstonePower(world, touch.pos(), touch.awayFromWire()) > 0;
    }
    for (Direction direction : DIRECTIONS) {
      if (state.getWeakRedstonePower(world, touch.pos(), direction) > 0) {
        return true;
      }
    }
    return false;
  }

  private static boolean receives(ServerWorld world, BlockPos pos) {
    for (Direction direction : DIRECTIONS) {
      BlockPos neighbour = pos.offset(direction);
      BlockState state = world.getBlockState(neighbour);
      if (state.isOf(Blocks.REDSTONE_WIRE)
          ? fedDustPowers(world, neighbour, state, direction)
          : emitsWithoutDust(world, neighbour, direction)
              || conductsFedDust(world, neighbour, state)) {
        return true;
      }
    }
    return false;
  }

  private static boolean fedDustPowers(
      ServerWorld world, BlockPos dust, BlockState state, Direction direction) {
    return state.getWeakRedstonePower(world, dust, direction) > 0 && dustIsFed(world, dust);
  }

  private static boolean conductsFedDust(ServerWorld world, BlockPos pos, BlockState state) {
    if (!state.isSolidBlock(world, pos)) {
      return false;
    }
    for (Direction direction : DIRECTIONS) {
      BlockPos neighbour = pos.offset(direction);
      BlockState neighbourState = world.getBlockState(neighbour);
      if (neighbourState.isOf(Blocks.REDSTONE_WIRE)
          && fedDustPowers(world, neighbour, neighbourState, direction)) {
        return true;
      }
    }
    return false;
  }

  public static boolean isReadable(ServerWorld world, BlockPos pos) {
    return world.isRegionLoaded(
        pos.add(-READ_REACH, 0, -READ_REACH), pos.add(READ_REACH, 0, READ_REACH));
  }

  public static boolean isUpdatable(ServerWorld world, BlockPos pos) {
    return world.isRegionLoaded(
        pos.add(-UPDATE_REACH, 0, -UPDATE_REACH), pos.add(UPDATE_REACH, 0, UPDATE_REACH));
  }

  public static boolean readsFrom(BlockPos pos, ChunkPos chunk) {
    return reaches(pos, chunk, READ_REACH);
  }

  public static boolean updatesInto(BlockPos pos, ChunkPos chunk) {
    return reaches(pos, chunk, UPDATE_REACH);
  }

  private static boolean reaches(BlockPos pos, ChunkPos chunk, int reach) {
    return chunk.x >= (pos.getX() - reach) >> 4
        && chunk.x <= (pos.getX() + reach) >> 4
        && chunk.z >= (pos.getZ() - reach) >> 4
        && chunk.z <= (pos.getZ() + reach) >> 4;
  }

  private static boolean emitsWithoutDust(ServerWorld world, BlockPos pos, Direction direction) {
    RedstoneWireBlock dust = (RedstoneWireBlock) Blocks.REDSTONE_WIRE;
    dust.wiresGivePower = false;
    try {
      return world.getEmittedRedstonePower(pos, direction) > 0;
    } finally {
      dust.wiresGivePower = true;
    }
  }

  private static boolean dustIsFed(ServerWorld world, BlockPos start) {
    Set<BlockPos> visited = new HashSet<>();
    Deque<BlockPos> frontier = new ArrayDeque<>();
    frontier.add(start);
    visited.add(start);
    for (int depth = 0; depth < DUST_REACH && !frontier.isEmpty(); depth++) {
      for (int count = frontier.size(); count > 0; count--) {
        BlockPos dust = frontier.poll();
        if (hasOtherSource(world, dust)) {
          return true;
        }
        for (Direction direction : Direction.Type.HORIZONTAL) {
          for (int rise = -1; rise <= 1; rise++) {
            BlockPos next = dust.offset(direction).up(rise);
            if (world.getBlockState(next).isOf(Blocks.REDSTONE_WIRE) && visited.add(next)) {
              frontier.add(next);
            }
          }
        }
      }
    }
    return false;
  }

  private static boolean hasOtherSource(ServerWorld world, BlockPos dust) {
    for (Direction direction : DIRECTIONS) {
      BlockPos neighbour = dust.offset(direction);
      if (!world.getBlockState(neighbour).isOf(Blocks.REDSTONE_WIRE)
          && emitsWithoutDust(world, neighbour, direction)) {
        return true;
      }
    }
    return false;
  }
}
