package com.grahambartley.hardwired.client.anim;

import net.minecraft.block.BlockState;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;

public record DeviceTurn(int x, int y) {

  private static final int NO_TURN = 0;
  private static final int WALL = 90;
  private static final int CEILING = 180;

  public static DeviceTurn of(BlockState state, boolean upright) {
    Direction facing =
        state.contains(Properties.HORIZONTAL_FACING)
            ? state.get(Properties.HORIZONTAL_FACING)
            : Direction.NORTH;
    BlockFace face =
        state.contains(Properties.BLOCK_FACE) ? state.get(Properties.BLOCK_FACE) : BlockFace.FLOOR;
    return of(face, facing, upright);
  }

  static DeviceTurn of(BlockFace face, Direction facing, boolean upright) {
    int yaw = yaw(facing);
    return switch (face) {
      case FLOOR -> new DeviceTurn(NO_TURN, yaw);
      case WALL -> new DeviceTurn(upright ? NO_TURN : WALL, yaw);
      case CEILING -> new DeviceTurn(CEILING, (yaw + CEILING) % 360);
    };
  }

  private static int yaw(Direction facing) {
    return switch (facing) {
      case EAST -> 90;
      case SOUTH -> 180;
      case WEST -> 270;
      default -> 0;
    };
  }
}
