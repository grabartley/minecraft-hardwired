package com.grahambartley.hardwired.client.glow;

import net.minecraft.block.BlockRenderType;
import net.minecraft.util.shape.VoxelShape;

final class PartShapes {

  private PartShapes() {}

  static boolean takesTint(
      BlockRenderType renderType, boolean hasBlockEntity, VoxelShape collision) {
    return renderType == BlockRenderType.MODEL && !hasBlockEntity && !collision.isEmpty();
  }
}
