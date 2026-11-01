package com.grahambartley.hardwired.radio;

public final class SongClock {

  public static final long STOPPED = -1;
  private static final long MILLIS_PER_TICK = 50;

  private SongClock() {}

  public static long millisIn(long now, long songStart) {
    return Math.max(0, now - songStart) * MILLIS_PER_TICK;
  }

  public static boolean ended(long now, long songStart, long songTicks) {
    return now - songStart >= songTicks;
  }
}
