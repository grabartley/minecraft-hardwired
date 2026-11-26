package com.grahambartley.hardwired.wire;

import com.grahambartley.hardwired.device.WireDevice;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public final class DoorCells {

  static final int MAX_CELLS = 64;

  private static final Set<Property<?>> VANILLA_SIZED =
      Set.copyOf(Blocks.OAK_DOOR.getStateManager().getProperties());

  private DoorCells() {}

  public static boolean fits(BlockState state) {
    return state.contains(DoorBlock.FACING) && state.contains(DoorBlock.HALF);
  }

  public static List<BlockPos> of(
      BlockPos pos, BlockState state, Function<BlockPos, BlockState> states) {
    if (Set.copyOf(state.getProperties()).equals(VANILLA_SIZED)) {
      return WireDevice.twoTall(pos, state.get(DoorBlock.HALF));
    }
    Direction facing = state.get(DoorBlock.FACING);
    Direction back = facing.rotateYCounterclockwise();
    Direction[] steps = {Direction.UP, Direction.DOWN, facing.rotateYClockwise(), back};
    BlockPos anchor = anchorOf(pos, state, states, back);
    Set<BlockPos> found = new HashSet<>();
    Deque<BlockPos> frontier = new ArrayDeque<>();
    found.add(anchor);
    frontier.add(anchor);
    while (!frontier.isEmpty()) {
      BlockPos cell = frontier.poll();
      for (Direction step : steps) {
        BlockPos next = cell.offset(step);
        if (found.size() < MAX_CELLS && sameDoor(states.apply(next), state) && found.add(next)) {
          frontier.add(next);
        }
      }
    }
    found.add(pos.toImmutable());
    found.remove(anchor);
    List<BlockPos> cells = new ArrayList<>();
    cells.add(anchor);
    found.stream().sorted().forEach(cells::add);
    return cells;
  }

  private static BlockPos anchorOf(
      BlockPos pos, BlockState state, Function<BlockPos, BlockState> states, Direction back) {
    BlockPos anchor = pos.toImmutable();
    for (int moves = 0; moves < MAX_CELLS; moves++) {
      if (sameDoor(states.apply(anchor.down()), state)) {
        anchor = anchor.down();
      } else if (sameDoor(states.apply(anchor.offset(back)), state)) {
        anchor = anchor.offset(back);
      } else {
        break;
      }
    }
    return anchor;
  }

  private static boolean sameDoor(BlockState other, BlockState door) {
    return other != null
        && other.isOf(door.getBlock())
        && other.get(DoorBlock.FACING) == door.get(DoorBlock.FACING);
  }
}
