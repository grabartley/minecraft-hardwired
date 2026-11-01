package com.grahambartley.hardwired.client.glow;

import net.minecraft.block.BlockRenderType;

final class PartShapes {

  private PartShapes() {}

  static boolean isPlainModel(BlockRenderType renderType, boolean hasBlockEntity) {
    return renderType == BlockRenderType.MODEL && !hasBlockEntity;
  }
}
