package com.grahambartley.hardwired.client.line;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public record SpanCurve(Vec3d from, Vec3d to, double sag) {

  public static final double ARM_HEIGHT = 13.0 / 16.0;
  public static final double LANE_GAP = 3.0 / 16.0;
  private static final double SAG_PER_BLOCK = 0.025;
  private static final double MOST_SAG = 1.0;

  public static SpanCurve between(BlockPos fromTop, BlockPos toTop, int lane, int lanes) {
    Vec3d from = Vec3d.ofBottomCenter(fromTop).add(0, ARM_HEIGHT, 0);
    Vec3d to = Vec3d.ofBottomCenter(toTop).add(0, ARM_HEIGHT, 0);
    Vec3d across = new Vec3d(to.z - from.z, 0, from.x - to.x);
    double length = across.length();
    if (length > 0) {
      across = across.multiply((lane - (lanes - 1) / 2.0) * LANE_GAP / length);
    }
    double sag = Math.min(MOST_SAG, from.distanceTo(to) * SAG_PER_BLOCK);
    return new SpanCurve(from.add(across), to.add(across), sag);
  }

  public Vec3d at(double t) {
    Vec3d straight = from.lerp(to, t);
    return straight.add(0, -4 * sag * t * (1 - t), 0);
  }

  public Vec3d low() {
    return new Vec3d(Math.min(from.x, to.x), Math.min(from.y, to.y) - sag, Math.min(from.z, to.z));
  }

  public Vec3d high() {
    return new Vec3d(Math.max(from.x, to.x), Math.max(from.y, to.y), Math.max(from.z, to.z));
  }
}
