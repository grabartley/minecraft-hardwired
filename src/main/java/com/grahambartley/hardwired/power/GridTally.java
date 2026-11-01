package com.grahambartley.hardwired.power;

import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.List;

public final class GridTally {

  private GridTally() {}

  public static void addLoad(Long2IntOpenHashMap loads, long anchor, int load) {
    loads.mergeInt(anchor, load, Math::max);
  }

  public static Long2IntOpenHashMap loads(List<Long2IntOpenHashMap> each) {
    Long2IntOpenHashMap merged = new Long2IntOpenHashMap();
    for (Long2IntOpenHashMap loads : each) {
      loads.long2IntEntrySet().fastForEach(e -> addLoad(merged, e.getLongKey(), e.getIntValue()));
    }
    return merged;
  }

  public static Long2IntOpenHashMap union(List<Long2IntOpenHashMap> each) {
    Long2IntOpenHashMap merged = new Long2IntOpenHashMap();
    each.forEach(merged::putAll);
    return merged;
  }

  public static int total(Long2IntOpenHashMap loads) {
    return loads.values().intStream().sum();
  }

  public static boolean runs(SourceState state, boolean waiting, boolean masterOpen) {
    return !masterOpen && !waiting && state == SourceState.READY;
  }

  public static GridRule.Count count(
      Long2IntOpenHashMap loads,
      Long2IntOpenHashMap ratings,
      Long2IntOpenHashMap sources,
      LongSet waiting,
      boolean masterOpen) {
    int supply = 0;
    int switchedOff = 0;
    boolean starting = false;
    for (Long2IntMap.Entry entry : sources.long2IntEntrySet()) {
      SourceState state = SourceState.byIndex(entry.getIntValue());
      boolean delayed = waiting.contains(entry.getLongKey());
      starting |= delayed;
      if (masterOpen || state == SourceState.SWITCHED_OFF) {
        switchedOff++;
      } else if (runs(state, delayed, false)) {
        supply += ratings.get(entry.getLongKey());
      }
    }
    return new GridRule.Count(total(loads), supply, sources.size(), switchedOff, starting);
  }
}
