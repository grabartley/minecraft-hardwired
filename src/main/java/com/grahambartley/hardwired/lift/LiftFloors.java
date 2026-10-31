package com.grahambartley.hardwired.lift;

import java.util.Collection;
import java.util.List;
import java.util.OptionalInt;

public final class LiftFloors {

  public static final int MAX_FLOORS = 256;

  private LiftFloors() {}

  public static List<Integer> ordered(Collection<Integer> levels) {
    return levels.stream().distinct().sorted().limit(MAX_FLOORS).toList();
  }

  public static int number(List<Integer> ordered, int level) {
    return ordered.indexOf(level) + 1;
  }

  public static OptionalInt atOrBelow(List<Integer> ordered, double level) {
    return ordered.stream().mapToInt(Integer::intValue).filter(l -> l <= level).max();
  }

  public static OptionalInt atOrAbove(List<Integer> ordered, double level) {
    return ordered.stream().mapToInt(Integer::intValue).filter(l -> l >= level).min();
  }

  public static OptionalInt lastSafe(List<Integer> ordered, double level, int direction) {
    return direction > 0 ? atOrBelow(ordered, level) : atOrAbove(ordered, level);
  }
}
