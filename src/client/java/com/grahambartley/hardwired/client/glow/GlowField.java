package com.grahambartley.hardwired.client.glow;

import com.grahambartley.hardwired.light.Glow;
import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.function.Predicate;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

final class GlowField {

  static final int RED = 0;
  static final int GREEN = 1;
  static final int BLUE = 2;
  static final int WEIGHT = 3;
  static final float MAX_LEVEL = 15F;
  private static final int CHANNELS = 4;

  private final Predicate<BlockPos> blocksLight;
  private final Long2ObjectOpenHashMap<float[]> cells = new Long2ObjectOpenHashMap<>();
  private final Long2BooleanOpenHashMap blocked = new Long2BooleanOpenHashMap();
  private final BlockPos.Mutable cursor = new BlockPos.Mutable();

  GlowField(Predicate<BlockPos> blocksLight) {
    this.blocksLight = blocksLight;
  }

  void add(Glow glow) {
    float[] hue = GlowColor.saturated(glow.rgb());
    Long2IntOpenHashMap reached = new Long2IntOpenHashMap();
    LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
    long origin = glow.pos().asLong();
    reached.put(origin, glow.level());
    queue.enqueue(origin);
    while (!queue.isEmpty()) {
      long pos = queue.dequeueLong();
      int level = reached.get(pos);
      mix(pos, hue, level / MAX_LEVEL);
      if (level > 1) {
        spread(pos, level - 1, reached, queue);
      }
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

  private void spread(long pos, int level, Long2IntOpenHashMap reached, LongArrayFIFOQueue queue) {
    for (Direction side : Direction.values()) {
      long next = BlockPos.offset(pos, side);
      if (!reached.containsKey(next) && !blocked(next)) {
        reached.put(next, level);
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
