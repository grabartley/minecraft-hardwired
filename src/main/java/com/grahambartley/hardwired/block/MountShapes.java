package com.grahambartley.hardwired.block;

import net.minecraft.block.Block;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;

public final class MountShapes {

  private static final double FULL = 16;

  private MountShapes() {}

  public static double[] mount(double[] floorBox, BlockFace face, Direction facing) {
    double[] box = floorBox;
    if (face == BlockFace.WALL) {
      box = new double[] {box[0], box[2], FULL - box[4], box[3], box[5], FULL - box[1]};
    } else if (face == BlockFace.CEILING) {
      box =
          new double[] {box[0], FULL - box[4], FULL - box[5], box[3], FULL - box[1], FULL - box[2]};
    }
    return turn(box, turns(face, facing));
  }

  public static VoxelShape shape(double[] floorBox, BlockFace face, Direction facing) {
    double[] box = mount(floorBox, face, facing);
    return Block.createCuboidShape(box[0], box[1], box[2], box[3], box[4], box[5]);
  }

  private static int turns(BlockFace face, Direction facing) {
    int turns = facing.getHorizontal() + 2;
    return face == BlockFace.CEILING ? turns + 2 : turns;
  }

  private static double[] turn(double[] box, int quarterTurns) {
    double[] turned = box;
    for (int i = 0; i < Math.floorMod(quarterTurns, 4); i++) {
      turned =
          new double[] {
            FULL - turned[5], turned[1], turned[0], FULL - turned[2], turned[4], turned[3]
          };
    }
    return turned;
  }
}
