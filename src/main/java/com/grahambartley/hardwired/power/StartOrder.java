package com.grahambartley.hardwired.power;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import java.util.Comparator;
import java.util.List;
import net.minecraft.util.math.BlockPos;

public final class StartOrder {

  public static final int START_TICKS = 20;

  private StartOrder() {}

  public static Long2LongOpenHashMap schedule(List<BlockPos> sources, BlockPos master, long now) {
    List<BlockPos> nearestFirst =
        sources.stream()
            .sorted(
                Comparator.<BlockPos>comparingDouble(pos -> pos.getSquaredDistance(master))
                    .thenComparing(BlockPos::asLong))
            .toList();
    Long2LongOpenHashMap starts = new Long2LongOpenHashMap();
    int count = nearestFirst.size();
    for (int rank = 0; rank < count; rank++) {
      starts.put(nearestFirst.get(rank).asLong(), now + (rank + 1L) * START_TICKS / count);
    }
    return starts;
  }
}
