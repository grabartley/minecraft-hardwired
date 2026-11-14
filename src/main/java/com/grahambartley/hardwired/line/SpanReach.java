package com.grahambartley.hardwired.line;

import com.grahambartley.hardwired.wire.WireSpan;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import net.minecraft.util.math.BlockPos;

public final class SpanReach {

  private SpanReach() {}

  public static double distanceSquared(WireSpan span, BlockPos at) {
    double ax = span.from().getX();
    double az = span.from().getZ();
    double dx = span.to().getX() - ax;
    double dz = span.to().getZ() - az;
    double lengthSquared = dx * dx + dz * dz;
    double t =
        lengthSquared == 0
            ? 0
            : Math.clamp(((at.getX() - ax) * dx + (at.getZ() - az) * dz) / lengthSquared, 0, 1);
    double nearX = ax + t * dx - at.getX();
    double nearZ = az + t * dz - at.getZ();
    return nearX * nearX + nearZ * nearZ;
  }

  public static List<WireSpan> near(Collection<WireSpan> spans, BlockPos at, int range, int most) {
    double rangeSquared = (double) range * range;
    return spans.stream()
        .filter(span -> distanceSquared(span, at) <= rangeSquared)
        .sorted(Comparator.comparingDouble(span -> distanceSquared(span, at)))
        .limit(most)
        .toList();
  }
}
