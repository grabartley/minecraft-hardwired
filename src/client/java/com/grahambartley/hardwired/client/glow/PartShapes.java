package com.grahambartley.hardwired.client.glow;

import net.minecraft.block.BlockRenderType;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

final class PartShapes {

  private PartShapes() {}

  static boolean reachesCellEdge(Box box, Direction side) {
    return side.getDirection() == Direction.AxisDirection.POSITIVE
        ? box.getMax(side.getAxis()) >= 1D
        : box.getMin(side.getAxis()) <= 0D;
  }

  static boolean isPlainModel(BlockRenderType renderType, boolean hasBlockEntity) {
    return renderType == BlockRenderType.MODEL && !hasBlockEntity;
  }
}
