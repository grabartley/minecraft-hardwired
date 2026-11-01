package com.grahambartley.hardwired.block;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

public final class DeskShapes {

  private static final double[][] DESK_BOXES = {
    {0, 5, 1, 16, 6, 15},
    {1, 0, 2, 3, 5, 4},
    {1, 0, 12, 3, 5, 14},
    {1, 6, 1, 15, 7.25, 14},
    {1.75, 7.25, 11, 2.75, 8.5, 12}
  };
  private static final double[][] PANEL_BOXES = {
    {0, 5, 1, 16, 6, 15},
    {13, 0, 2, 15, 5, 4},
    {13, 0, 12, 15, 5, 14},
    {0, 6, 10.75, 16, 16, 13},
    {11, 8, 10, 14, 11, 10.75},
    {1.75, 6, 3.5, 6.75, 7.25, 8.5},
    {0.125, 7.25, 5.5, 8.375, 15.625, 6.5}
  };
  private static final double[][] TOP_BOXES = {
    {0, 0, 10.75, 16, 9, 13},
    {0, 9, 9.75, 16, 10, 14},
    {0.5, 10, 10.5, 3.5, 16, 13.5},
    {4.5, 10, 10.5, 7.5, 16, 13.5},
    {8.5, 10, 10.5, 11.5, 16, 13.5},
    {12.5, 10, 10.5, 15.5, 16, 13.5}
  };
  private static final Map<DeskPart, Map<Direction, VoxelShape>> SHAPES = shapes();

  private DeskShapes() {}

  public static VoxelShape shape(DeskPart part, Direction facing) {
    return SHAPES.get(part).get(facing);
  }

  static double[][] boxes(DeskPart part, Direction facing) {
    double[][] authored =
        switch (part) {
          case DESK -> DESK_BOXES;
          case PANEL -> PANEL_BOXES;
          case TOP -> TOP_BOXES;
        };
    double[][] turned = new double[authored.length][];
    for (int i = 0; i < authored.length; i++) {
      turned[i] = MountShapes.turn(authored[i], facing.rotateYClockwise().getHorizontal());
    }
    return turned;
  }

  private static Map<DeskPart, Map<Direction, VoxelShape>> shapes() {
    Map<DeskPart, Map<Direction, VoxelShape>> shapes = new EnumMap<>(DeskPart.class);
    for (DeskPart part : DeskPart.values()) {
      Map<Direction, VoxelShape> byFacing = new EnumMap<>(Direction.class);
      for (Direction facing : Direction.Type.HORIZONTAL) {
        VoxelShape shape = VoxelShapes.empty();
        for (double[] box : boxes(part, facing)) {
          shape =
              VoxelShapes.union(
                  shape, Block.createCuboidShape(box[0], box[1], box[2], box[3], box[4], box[5]));
        }
        byFacing.put(facing, shape);
      }
      shapes.put(part, byFacing);
    }
    return shapes;
  }
}
