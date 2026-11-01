package com.grahambartley.hardwired.block;

import net.minecraft.block.Block;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

public final class StreetlightShapes {

  private static final double[][][] BOXES = {
    {
      {2, 0, 2, 14, 2, 14},
      {3, 2, 3, 13, 4, 13},
      {4, 4, 4, 12, 8, 12},
      {5, 8, 5, 11, 10, 11},
      {5.5, 10, 5.5, 10.5, 16, 10.5}
    },
    {{6, 0, 6, 10, 16, 10}, {5.5, 7, 5.5, 10.5, 8, 10.5}},
    {
      {7, 0, 7, 9, 16, 9},
      {3, 11.5, 7.5, 13, 13, 8.5},
      {6.5, 11, 6.5, 9.5, 13.5, 9.5},
      {5.5, 14, 5.5, 10.5, 16, 10.5}
    },
    {{3, 0, 3, 13, 16, 13}}
  };
  private static final VoxelShape[] SHAPES = shapes();

  private StreetlightShapes() {}

  public static VoxelShape shape(int part) {
    return SHAPES[part];
  }

  static double[][] boxes(int part) {
    return BOXES[part];
  }

  private static VoxelShape[] shapes() {
    VoxelShape[] shapes = new VoxelShape[BOXES.length];
    for (int part = 0; part < BOXES.length; part++) {
      VoxelShape shape = VoxelShapes.empty();
      for (double[] box : BOXES[part]) {
        shape =
            VoxelShapes.union(
                shape, Block.createCuboidShape(box[0], box[1], box[2], box[3], box[4], box[5]));
      }
      shapes[part] = shape;
    }
    return shapes;
  }
}
