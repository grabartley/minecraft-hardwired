package com.grahambartley.hardwired.lift;

import java.util.List;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public final class LiftGateCells {

  private static final double MIDDLE = 0.5;

  private LiftGateCells() {}

  private static Direction along(Direction facing) {
    return facing.rotateYClockwise();
  }

  public static BlockPos origin(BlockPos lower, Direction facing, boolean second) {
    return second ? lower.offset(along(facing).getOpposite()) : lower;
  }

  public static List<BlockPos> cells(BlockPos origin, Direction facing) {
    BlockPos second = origin.offset(along(facing));
    return List.of(origin, origin.up(), second, second.up());
  }

  public static List<BlockPos> columns(BlockPos origin, Direction facing) {
    BlockPos first = origin.offset(facing);
    return List.of(first, first.offset(along(facing)));
  }

  public static boolean placedAsSecond(BlockPos pos, Direction facing, Vec3d hit) {
    Direction along = along(facing);
    double aim =
        (hit.x - pos.getX() - MIDDLE) * along.getOffsetX()
            + (hit.z - pos.getZ() - MIDDLE) * along.getOffsetZ();
    return aim < 0;
  }
}
