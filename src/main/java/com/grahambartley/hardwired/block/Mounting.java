package com.grahambartley.hardwired.block;

import java.util.Set;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.WallMountedBlock;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.WorldView;

public final class Mounting {

  public static final EnumProperty<BlockFace> FACE = Properties.BLOCK_FACE;
  public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;

  private Mounting() {}

  public static BlockState placement(
      BlockState base, Set<BlockFace> faces, ItemPlacementContext context) {
    for (Direction direction : context.getPlacementDirections()) {
      BlockFace face = faceFor(direction);
      if (!faces.contains(face)) {
        continue;
      }
      Direction facing =
          direction.getAxis() == Direction.Axis.Y
              ? context.getHorizontalPlayerFacing()
              : direction.getOpposite();
      BlockState state = base.with(FACE, face).with(FACING, facing);
      if (canPlaceAt(state, context.getWorld(), context.getBlockPos())) {
        return state;
      }
    }
    return null;
  }

  public static boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
    return WallMountedBlock.canPlaceAt(world, pos, attachedDirection(state));
  }

  public static BlockState afterNeighborChange(
      BlockState state, Direction direction, WorldView world, BlockPos pos) {
    return direction == attachedDirection(state) && !canPlaceAt(state, world, pos)
        ? Blocks.AIR.getDefaultState()
        : state;
  }

  public static BlockFace faceFor(Direction direction) {
    return switch (direction) {
      case UP -> BlockFace.CEILING;
      case DOWN -> BlockFace.FLOOR;
      default -> BlockFace.WALL;
    };
  }

  public static Direction attachedDirection(BlockState state) {
    return attachedDirection(state.get(FACE), state.get(FACING));
  }

  public static Direction attachedDirection(BlockFace face, Direction facing) {
    return switch (face) {
      case CEILING -> Direction.UP;
      case FLOOR -> Direction.DOWN;
      case WALL -> facing.getOpposite();
    };
  }
}
