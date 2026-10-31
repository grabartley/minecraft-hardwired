package com.grahambartley.hardwired.block;

import net.minecraft.block.enums.BlockFace;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

final class CircuitBoardShape {

  private static final double[] DECK_BOX = {0, 0, 0, 16, 9, 16};
  private static final double[] REAR_BOX = {0, 9, 8, 16, 12, 16};

  private CircuitBoardShape() {}

  static VoxelShape of(Direction facing) {
    return VoxelShapes.union(
        MountShapes.shape(DECK_BOX, BlockFace.FLOOR, facing),
        MountShapes.shape(REAR_BOX, BlockFace.FLOOR, facing));
  }
}
