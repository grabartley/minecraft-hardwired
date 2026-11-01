package com.grahambartley.hardwired.client;

import com.grahambartley.hardwired.network.WireChunkPayload.CellView;
import com.grahambartley.hardwired.wire.WireTopology;
import java.util.List;
import java.util.Optional;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class WireLanes {

  private static final double STEP = 3.0 / 32.0;
  private static final double WIDEST_SPREAD = 0.8;
  private static final double ARM_LENGTH = 0.5;

  private WireLanes() {}

  static double offset(int lane, int lanes) {
    if (lanes < 2) {
      return 0;
    }
    double step = Math.min(STEP, WIDEST_SPREAD / (lanes - 1));
    return (lane - (lanes - 1) / 2.0) * step;
  }

  public static double[] offsets(List<CellView> sorted) {
    double[] offsets = new double[sorted.size()];
    int start = 0;
    while (start < sorted.size()) {
      int end = start;
      int lanes = 0;
      while (end < sorted.size() && sameFace(sorted.get(start), sorted.get(end))) {
        lanes += end == start || sorted.get(end).color() != sorted.get(end - 1).color() ? 1 : 0;
        end++;
      }
      int lane = 0;
      for (int cell = start; cell < end; cell++) {
        lane += cell > start && sorted.get(cell).color() != sorted.get(cell - 1).color() ? 1 : 0;
        offsets[cell] = offset(lane, lanes);
      }
      start = end;
    }
    return offsets;
  }

  private static boolean sameFace(CellView first, CellView second) {
    return first.pos() == second.pos() && first.side() == second.side();
  }

  public static Vec3d shift(Direction side, double offset) {
    return new Vec3d(
        side.getOffsetX() == 0 ? offset : 0,
        side.getOffsetY() == 0 ? offset : 0,
        side.getOffsetZ() == 0 ? offset : 0);
  }

  public static Optional<CellView> nearest(List<CellView> sorted, Vec3d hit) {
    double[] offsets = offsets(sorted);
    Optional<CellView> nearest = Optional.empty();
    double shortest = Double.POSITIVE_INFINITY;
    for (int index = 0; index < offsets.length; index++) {
      double distance = squaredDistance(sorted.get(index), offsets[index], hit);
      if (distance < shortest) {
        shortest = distance;
        nearest = Optional.of(sorted.get(index));
      }
    }
    return nearest;
  }

  private static double squaredDistance(CellView cell, double offset, Vec3d hit) {
    Direction side = Direction.byId(cell.side());
    Vec3d normal = Vec3d.of(side.getVector());
    Vec3d center = Vec3d.ofCenter(BlockPos.fromLong(cell.pos())).add(shift(side, offset));
    Vec3d toHit = hit.subtract(center);
    Vec3d onFace = toHit.subtract(normal.multiply(toHit.dotProduct(normal)));
    double shortest = onFace.lengthSquared();
    for (Direction arm : WireTopology.edges(side)) {
      if (cell.hasArm(arm)) {
        Vec3d along = Vec3d.of(arm.getVector());
        double reach = MathHelper.clamp(onFace.dotProduct(along), 0, ARM_LENGTH);
        shortest = Math.min(shortest, onFace.subtract(along.multiply(reach)).lengthSquared());
      }
    }
    return shortest;
  }
}
