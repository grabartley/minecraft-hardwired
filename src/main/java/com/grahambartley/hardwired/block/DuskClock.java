package com.grahambartley.hardwired.block;

public final class DuskClock {

  public static final int DUSK = 12500;
  public static final int DAWN = 23500;
  private static final int DAY_LENGTH = 24000;

  private DuskClock() {}

  public static boolean isDark(long timeOfDay) {
    long time = Math.floorMod(timeOfDay, DAY_LENGTH);
    return time >= DUSK && time < DAWN;
  }
}
