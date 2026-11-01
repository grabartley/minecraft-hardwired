package com.grahambartley.hardwired.client.glow;

import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

final class PartShapes {

  private PartShapes() {}

  static boolean sameShape(VoxelShape outline, VoxelShape collision) {
    return !outline.isEmpty()
        && !VoxelShapes.matchesAnywhere(outline, collision, BooleanBiFunction.NOT_SAME);
  }
}
