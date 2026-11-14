package com.grahambartley.hardwired.block;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

public final class PhoneBoxShapes {

  private static final double WALL = 1;
  private static final double FULL = 16;

  private static final Map<Direction, VoxelShape[]> SHAPES = new EnumMap<>(Direction.class);

  static {
    for (Direction facing : Direction.Type.HORIZONTAL) {
      VoxelShape[] shapes = new VoxelShape[PhoneBoxPart.values().length * 2];
      for (PhoneBoxPart part : PhoneBoxPart.values()) {
        for (boolean open : new boolean[] {false, true}) {
          shapes[index(part, open)] = turn(north(part, open), facing);
        }
      }
      SHAPES.put(facing, shapes);
    }
  }

  private PhoneBoxShapes() {}

  public static VoxelShape shape(PhoneBoxPart part, Direction facing, boolean open) {
    return SHAPES.get(facing)[index(part, open)];
  }

  public static double depthFromDoor(double x, double z, Direction facing) {
    return switch (facing) {
      case SOUTH -> 1 - z;
      case EAST -> 1 - x;
      case WEST -> x;
      default -> z;
    };
  }

  private static int index(PhoneBoxPart part, boolean open) {
    return part.ordinal() * 2 + (open ? 1 : 0);
  }

  private static double[][] north(PhoneBoxPart part, boolean open) {
    if (part == PhoneBoxPart.UPPER) {
      return new double[][] {{0, 0, 0, FULL, FULL, FULL}};
    }
    double floor = part == PhoneBoxPart.LOWER ? WALL : 0;
    double[][] walls = {
      {0, 0, 0, FULL, floor, FULL},
      {0, 0, 0, WALL, FULL, FULL},
      {FULL - WALL, 0, 0, FULL, FULL, FULL},
      {0, 0, FULL - WALL, FULL, FULL, FULL}
    };
    if (open) {
      return walls;
    }
    double[][] shut = Arrays.copyOf(walls, walls.length + 1);
    shut[walls.length] = new double[] {WALL, floor, 0, FULL - WALL, FULL, WALL};
    return shut;
  }

  private static VoxelShape turn(double[][] boxes, Direction facing) {
    VoxelShape shape = VoxelShapes.empty();
    for (double[] box : boxes) {
      double[] turned = MountShapes.turn(box, facing.getHorizontal() + 2);
      if (turned[0] < turned[3] && turned[1] < turned[4] && turned[2] < turned[5]) {
        shape =
            VoxelShapes.union(
                shape,
                Block.createCuboidShape(
                    turned[0], turned[1], turned[2], turned[3], turned[4], turned[5]));
      }
    }
    return shape;
  }
}
