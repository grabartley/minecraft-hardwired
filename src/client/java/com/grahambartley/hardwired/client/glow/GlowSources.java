package com.grahambartley.hardwired.client.glow;

import com.grahambartley.hardwired.light.Glow;
import java.util.Comparator;
import java.util.List;
import net.minecraft.util.math.BlockPos;

final class GlowSources {

  private GlowSources() {}

  static List<Glow> nearest(List<Glow> glows, BlockPos center, int limit) {
    return glows.stream()
        .sorted(
            Comparator.comparingDouble((Glow glow) -> glow.pos().getSquaredDistance(center))
                .thenComparingLong(glow -> glow.pos().asLong()))
        .limit(limit)
        .toList();
  }
}
