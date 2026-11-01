package com.grahambartley.hardwired.power;

public final class GridRule {

  public static final int MIN_DARK_TICKS = 40;

  public record Count(int load, int supply, int sources, int switchedOff, boolean starting) {}

  private GridRule() {}

  public static GridState next(Count count, long now, long darkUntil) {
    if (count.starting()) {
      return GridState.RETURNING;
    }
    if (count.supply() <= 0) {
      return stoppedWith(count);
    }
    if (count.load() > count.supply()) {
      return GridState.OVERLOADED;
    }
    return now < darkUntil ? GridState.RETURNING : GridState.LIVE;
  }

  public static long darkUntil(GridState before, GridState after, long now, long darkUntil) {
    return before.givesPower() && !after.givesPower() ? now + MIN_DARK_TICKS : darkUntil;
  }

  public static boolean blowsFuse(GridState before, GridState after) {
    return after == GridState.OVERLOADED && before != GridState.OVERLOADED;
  }

  private static GridState stoppedWith(Count count) {
    if (count.sources() == 0) {
      return GridState.NO_SOURCE;
    }
    return count.switchedOff() == count.sources() ? GridState.SOURCES_OFF : GridState.OUT_OF_FUEL;
  }
}
