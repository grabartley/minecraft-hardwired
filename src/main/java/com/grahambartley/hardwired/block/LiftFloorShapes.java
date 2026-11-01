package com.grahambartley.hardwired.block;

import net.minecraft.block.Block;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;

public final class LiftFloorShapes {

  private static final double THICKNESS = 3;
  private static final double FULL = 16;

  private LiftFloorShapes() {}

  public static VoxelShape closed(Direction shaftSide) {
    return span(shaftSide, 0, FULL);
  }

  private static VoxelShape span(Direction side, double from, double to) {
    return switch (side) {
      case NORTH -> Block.createCuboidShape(from, 0, 0, to, FULL, THICKNESS);
      case SOUTH -> Block.createCuboidShape(from, 0, FULL - THICKNESS, to, FULL, FULL);
      case WEST -> Block.createCuboidShape(0, 0, from, THICKNESS, FULL, to);
      default -> Block.createCuboidShape(FULL - THICKNESS, 0, from, FULL, FULL, to);
    };
  }
}
