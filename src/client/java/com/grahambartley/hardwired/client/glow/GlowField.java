package com.grahambartley.hardwired.client.glow;

import com.grahambartley.hardwired.light.Glow;
import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.function.Predicate;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

final class GlowField {

  static final int RED = 0;
  static final int GREEN = 1;
  static final int BLUE = 2;
  static final int WEIGHT = 3;
  private static final int CHANNELS = 4;
  private static final double CELL_CENTRE = 0.5D;

  private final Predicate<BlockPos> blocksLight;
  private final LightCost lightCost;
  private final Long2ObjectOpenHashMap<float[]> cells = new Long2ObjectOpenHashMap<>();
  private final Long2BooleanOpenHashMap blocked = new Long2BooleanOpenHashMap();
  private final BlockPos.Mutable cursor = new BlockPos.Mutable();

  GlowField(Predicate<BlockPos> blocksLight, LightCost lightCost) {
    this.blocksLight = blocksLight;
    this.lightCost = lightCost;
  }

  void add(Glow glow) {
    float[] hue = GlowColor.channels(glow.rgb());
    Long2IntOpenHashMap reached = new Long2IntOpenHashMap();
    LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
    long origin = glow.pos().asLong();
    reached.put(origin, glow.level());
    queue.enqueue(origin);
    while (!queue.isEmpty()) {
      long pos = queue.dequeueLong();
      spread(pos, reached.get(pos), reached, queue);
    }
    for (Long2IntMap.Entry entry : reached.long2IntEntrySet()) {
      mix(entry.getLongKey(), hue, (float) entry.getIntValue() / Glow.MAX_LEVEL);
    }
  }

  boolean blocksLight(BlockPos pos) {
    return blocked(pos.asLong());
  }

  LongSet cells() {
    return cells.keySet();
  }

  float[] at(long pos) {
    return cells.get(pos);
  }

  float[] sample(double x, double y, double z) {
    double cornerX = x - CELL_CENTRE;
    double cornerY = y - CELL_CENTRE;
    double cornerZ = z - CELL_CENTRE;
    int baseX = MathHelper.floor(cornerX);
    int baseY = MathHelper.floor(cornerY);
    int baseZ = MathHelper.floor(cornerZ);
    float[] sum = new float[CHANNELS];
    float open = 0F;
    BlockPos.Mutable pos = new BlockPos.Mutable();
    for (int dx = 0; dx <= 1; dx++) {
      for (int dy = 0; dy <= 1; dy++) {
        for (int dz = 0; dz <= 1; dz++) {
          float share =
              share(cornerX - baseX, dx) * share(cornerY - baseY, dy) * share(cornerZ - baseZ, dz);
          pos.set(baseX + dx, baseY + dy, baseZ + dz);
          if (share > 0F && !blocksLight(pos)) {
            open += share;
            addShare(sum, cells.get(pos.asLong()), share);
          }
        }
      }
    }
    for (int channel = 0; channel < CHANNELS && open > 0F; channel++) {
      sum[channel] /= open;
    }
    return sum;
  }

  private static float share(double fraction, int step) {
    return (float) (step == 1 ? fraction : 1D - fraction);
  }

  private static void addShare(float[] sum, float[] cell, float share) {
    for (int channel = 0; cell != null && channel < CHANNELS; channel++) {
      sum[channel] += cell[channel] * share;
    }
  }

  private void spread(long pos, int level, Long2IntOpenHashMap reached, LongArrayFIFOQueue queue) {
    BlockPos from = BlockPos.fromLong(pos);
    for (Direction side : Direction.values()) {
      long next = BlockPos.offset(pos, side);
      int left = level - lightCost.cost(from, side);
      if (left > reached.get(next)) {
        reached.put(next, left);
        queue.enqueue(next);
      }
    }
  }

  private boolean blocked(long pos) {
    if (blocked.containsKey(pos)) {
      return blocked.get(pos);
    }
    boolean result = blocksLight.test(cursor.set(pos));
    blocked.put(pos, result);
    return result;
  }

  private void mix(long pos, float[] hue, float strength) {
    float[] cell = cells.computeIfAbsent(pos, key -> new float[CHANNELS]);
    cell[RED] += hue[RED] * strength;
    cell[GREEN] += hue[GREEN] * strength;
    cell[BLUE] += hue[BLUE] * strength;
    cell[WEIGHT] += strength;
  }
}
