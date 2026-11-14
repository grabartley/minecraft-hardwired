package com.grahambartley.hardwired.client.line;

import com.grahambartley.hardwired.block.TelephonePoleBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public record SpanCurve(Vec3d from, Vec3d to, double sag) {

  public record End(BlockPos top, Direction.Axis arm, int lane, int lanes) {

    Vec3d attach() {
      double along = ((lane + 0.5) / lanes - 0.5) * TelephonePoleBlock.ARM_LENGTH;
      Vec3d centre = Vec3d.ofBottomCenter(top).add(0, TelephonePoleBlock.ARM_HEIGHT, 0);
      return arm == Direction.Axis.X ? centre.add(along, 0, 0) : centre.add(0, 0, along);
    }
  }

  private static final double SAG_PER_BLOCK = 0.025;
  private static final double MOST_SAG = 1.0;

  public static SpanCurve between(End from, End to) {
    Vec3d start = from.attach();
    Vec3d end = to.attach();
    return new SpanCurve(start, end, Math.min(MOST_SAG, start.distanceTo(end) * SAG_PER_BLOCK));
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
