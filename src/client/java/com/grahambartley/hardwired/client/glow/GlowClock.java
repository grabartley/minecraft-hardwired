package com.grahambartley.hardwired.client.glow;

final class GlowClock {

  private final int checkTicks;
  private final int rebuildTicks;
  private long checkedAt = Long.MIN_VALUE / 2;
  private long builtAt = Long.MIN_VALUE / 2;

  GlowClock(int checkTicks, int rebuildTicks) {
    this.checkTicks = checkTicks;
    this.rebuildTicks = rebuildTicks;
  }

  boolean checkDue(long time) {
    if (isRecent(time, checkedAt, checkTicks)) {
      return false;
    }
    checkedAt = time;
    return true;
  }

  boolean rebuildDue(long time, boolean changed) {
    if (!changed && isRecent(time, builtAt, rebuildTicks)) {
      return false;
    }
    builtAt = time;
    return true;
  }

  private static boolean isRecent(long time, long at, int ticks) {
    return time >= at && time - at < ticks;
  }
}
